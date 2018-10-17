package org.renjin.cran.rjson;


import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.renjin.sexp.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a JSON stream into a SEXP.
 * 
 * <p>The translation is meant to respect the semantics defined by the original rjson package.
 * Namely:
 * <ul>
 *     <li>JSON Objects are <em>always</em> read in as a ListVector with a names attribute</li>
 *     <li>Homogeneous arrays (all numbers, all booleans, or all strings) are read as atomic vectors</li>
 *     <li>Heterogeneous arrays (a mix of numbers, booleans, etc) are read as ListVectors</li>
 *     <li>Special numbers encoded as "NA" or "NaN" or "Inf" are <em>not</em> respected: they are <em>always</em>
 *     treated as normal strings.</li>
 * </ul>
 */
public class JsonParser {

    private static final int PRE_READ_COUNT = 25;

    public static SEXP parse(String json) throws IOException {
        StringReader reader = new StringReader(json);
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        return readSexp(jsonReader);
    }
    
    public static SEXP readSexp(JsonReader reader) throws IOException {
        switch (reader.peek()) {
            case BEGIN_ARRAY:
                return readArray(reader);
            case BEGIN_OBJECT:
                return readObject(reader);
            case STRING:
                return new StringArrayVector(reader.nextString());
            case NUMBER:
                return new DoubleArrayVector(reader.nextDouble());
            case BOOLEAN:
                return LogicalVector.valueOf(reader.nextBoolean());
            case NULL:
                reader.nextNull();
                return Null.INSTANCE;
            default:
                throw new IllegalStateException("Unexpected token " + reader.peek());
        }
    }

    /**
     * Reads a JSON Array as a SEXP
     */
    private static SEXP readArray(JsonReader reader) throws IOException {

        reader.beginArray();
        
        // pre-read first x elements, jumping out early if we encounter 
        // mixed items, or complex structures like array, object, or NULL
        List<Object> head = new ArrayList<Object>(PRE_READ_COUNT);
        
        int numeric = 0;
        int logical = 0;
        int strings = 0;
        
        nextElement: while(head.size() < PRE_READ_COUNT) {
            switch (reader.peek()) {
                case NUMBER:
                    head.add(reader.nextDouble());
                    numeric = 1;
                    break;
                
                case BOOLEAN:
                    head.add(reader.nextBoolean());
                    logical = 1;
                    break;
                
                case STRING:
                    String string = reader.nextString();
                    head.add(string);
                    strings = 1;
                    break;
                
                case NULL:
                case BEGIN_OBJECT:
                case BEGIN_ARRAY:
                    head.add(readSexp(reader));
                    return finishReadingListVector(reader, head);

                case END_ARRAY:
                    break nextElement;
                default:
                    throw new IllegalStateException("Unexpected token " + reader.peek());
            }
            // Do we have more than one distinct element type?
            // Then we must build a ListVector. Stop the exploration early
            // and read the rest of the array in as a ListVector
            if( (numeric + logical + strings) > 1) {
                return finishReadingListVector(reader, head);
            }
        }
        // By the convention introduced by rjson, a zero-length JSON array
        // is always read as a list.
        if(head.size() == 0) {
            reader.endArray();
            return ListVector.EMPTY;
        }
        if(numeric == 1) {
            return finishReadingNumericArray(reader, head);
        } else if(logical == 1) {
            return finishReadingLogicalArray(reader, head);
        } else if(strings == 1) {
            return finishReadingStringArray(reader, head);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Reads the rest of a JSON array as a logical vector. If we encounter anything besides a string element,
     * break out of the loop and read the rest as a list vector.
     *
     */
    private static SEXP finishReadingStringArray(JsonReader reader, List<Object> head) throws IOException {
        StringArrayVector.Builder vector = new StringArrayVector.Builder();
        for (Object x : head) {
            vector.add((String)x);
        }
        while (true) {
            switch (reader.peek()) {
                case STRING:
                    vector.add(reader.nextString());
                    break;
                case END_ARRAY:
                    reader.endArray();
                    return vector.build();
                
                default:
                    finishReadingArrayAsListAfterAll(reader, vector.build());
            }
        }
    }

    /**
     * Reads the rest of a JSON array as a logical vector. If we encounter anything besides a string element,
     * break out of the loop and read the rest as a list vector.
     *
     */
    public static LogicalVector finishReadingLogicalArray(JsonReader reader, List<Object> head) throws IOException {
        LogicalArrayVector.Builder vector = new LogicalArrayVector.Builder(0, head.size());
        for (Object x : head) {
            vector.add(x == Boolean.TRUE);
        }
        while (true) {
            switch (reader.peek()) {
                case BOOLEAN:
                    vector.add(reader.nextBoolean());
                    break;
                case END_ARRAY:
                    reader.endArray();
                    return vector.build();

                default:
                    finishReadingArrayAsListAfterAll(reader, vector.build());
            }
        }
    }

    /**
     * Reads the rest of a JSON array as a character vector. If we encounter anything besides a string element,
     * break out of the loop and read the rest as a list vector.
     * 
     */
    private static Vector finishReadingNumericArray(JsonReader reader, List<Object> head) throws IOException {
        DoubleVector.Builder vector = new DoubleArrayVector.Builder();
        for (Object x : head) {
            vector.add(((Number) x).doubleValue());
        }
        while (true) {
            switch (reader.peek()) {
                case NUMBER:
                    vector.add(reader.nextDouble());
                    break;
                case END_ARRAY:
                    reader.endArray();
                    return vector.build();

                default:
                    finishReadingArrayAsListAfterAll(reader, vector.build());
            }
        }
    }

    private static ListVector finishReadingListVector(JsonReader reader, List<Object> head) throws IOException {
        ListVector.Builder list = ListVector.newBuilder();
        for (Object x : head) {
            if(x instanceof SEXP) {
                list.add(((SEXP) x));
            } else if(x instanceof Number) {
                list.add(((Number) x));
            } else if(x instanceof String) {
                list.add(new StringArrayVector((String)x));
            }
        }
        return finishReadingArrayAsListVector(list, reader);
    }

    private static ListVector finishReadingArrayAsListAfterAll(JsonReader reader, Vector vector) throws IOException {
        ListVector.Builder list = new ListVector.Builder();
        for (int i = 0; i < vector.length(); i++) {
            list.add(vector.getElementAsSEXP(i));
        }
        return finishReadingArrayAsListVector(list, reader);
    }
    
    private static ListVector finishReadingArrayAsListVector(ListVector.Builder list, JsonReader reader) throws IOException {
        while(true) {
            switch (reader.peek()) {
                case NUMBER:
                    list.add(new DoubleArrayVector(reader.nextDouble()));
                    break;

                case BOOLEAN:
                    list.add(LogicalArrayVector.valueOf(reader.nextBoolean()));
                    break;

                case NULL:
                case BEGIN_ARRAY:
                case BEGIN_OBJECT:
                    list.add(readSexp(reader));
                    break;

                case STRING:
                    list.add(new StringArrayVector(reader.nextString()));
                    break;

                case END_ARRAY:
                    reader.endArray();
                    return list.build();

                default:
                    throw new IllegalStateException("Unexpected token: " + reader.peek());
            }
        }
    }


    /**
     * Reads a JSON Object as a list vector.
     */
    private static SEXP readObject(JsonReader reader) throws IOException {
        reader.beginObject();

        ListVector.NamedBuilder list = ListVector.newNamedBuilder();
        while(true) {
            JsonToken nextToken = reader.peek();

            if(nextToken == JsonToken.END_OBJECT) {
                reader.endObject();
                return list.build();

            } else {
                String name = reader.nextName();
                nextToken = reader.peek();
                switch (nextToken) {
                    case NUMBER:
                        list.add(name, new DoubleArrayVector(reader.nextDouble()));
                        break;
                    case BOOLEAN:
                        list.add(name, LogicalArrayVector.valueOf(reader.nextBoolean()));
                        break;
                    case STRING:
                        list.add(name, new StringArrayVector(reader.nextString()));
                        break;
                    case NULL:
                    case BEGIN_ARRAY:
                    case BEGIN_OBJECT:
                        list.add(name, readSexp(reader));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected token: " + nextToken);
                }
            }
        }
    }

}
