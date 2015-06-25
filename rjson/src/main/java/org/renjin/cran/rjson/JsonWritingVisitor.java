package org.renjin.cran.rjson;

import com.google.gson.stream.JsonWriter;
import org.renjin.primitives.Types;
import org.renjin.sexp.*;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;

/**
 * Serializes an R SEXP to JSON
 */
public class JsonWritingVisitor extends SexpVisitor<String> {

    private JsonWriter json;
    private StringWriter writer;
    
    
    public static StringVector toJson(SEXP x) {
        JsonWritingVisitor visitor = new JsonWritingVisitor();
        x.accept(visitor);
        return new StringArrayVector(visitor.writer.toString());
    }
    
    public JsonWritingVisitor() {
        writer = new StringWriter();
        json = new JsonWriter(writer);
        // Set the writer to 'lenient' to allow
        // strings/numbers/boolean etc as a top-level item
        json.setLenient(true);
    }

    
    @Override
    public void visit(Null nullExpression) {
        try {
            json.nullValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(LogicalVector vector) {
        try {
            if(vector.getAttributes().hasNames()) {
                writeNamedList(vector);
            } else if(vector.length() == 1) {
                writeLogical(vector);
            } else {
                json.beginArray();
                writeLogical(vector);
                json.endArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void visit(Symbol symbol) {
        try {
            json.value(symbol.getPrintName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void visit(IntVector vector) {
        if(vector.inherits("factor")) {
            visitFactor(vector);
        } else {
            try {
                if(vector.getAttributes().hasNames()) {
                    writeNamedList(vector);
                } else if(vector.length() == 1) {
                    writeIntVector(vector);
                } else {
                    json.beginArray();
                    writeIntVector(vector);
                    json.endArray();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void visitFactor(AtomicVector vector) {
        try {
            if(vector.length() == 1) {
                writeFactor(vector);
            } else {
                json.beginArray();
                writeFactor(vector);
                json.endArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void visit(DoubleVector vector) {
        try {
            if(vector.inherits("factor")) {
                visitFactor(vector);
            } else if(vector.getAttributes().hasNames()) {
                writeNamedList(vector);
            } else if(vector.length() == 1) {
                writeDouble(vector);
            } else {
                json.beginArray();
                writeDouble(vector);
                json.endArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
    }


    @Override
    public void visit(StringVector vector) {
        try {
            if(vector.length() == 1) {
                writeStringVector(vector);
            } else {
                json.beginArray();
                writeStringVector(vector);
                json.endArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }    
    }


    @Override
    public void visit(ExpressionVector vector) {
        visit((ListVector)vector);
    }

    @Override
    public void visit(ListVector list) {
        try {
            if(list.getAttributes().hasNames()) {
                writeNamedList(list);
            } else {
                json.beginArray();
                for(int i=0;i<list.length();++i) {
                    list.getElementAsSEXP(i).accept(this);
                }
                json.endArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void writeNamedList(Vector vector) throws IOException {
        json.beginObject();
        StringVector names = vector.getAttributes().getNames();
        for (int i = 0; i < vector.length(); i++) {
            json.name(names.getElementAsString(i));
            vector.getElementAsSEXP(i).accept(this);
        }
        json.endObject();
    }


    @Override
    public void visit(Environment environment) {
        try {
            json.beginObject();
            Frame frame = environment.getFrame();
            for (Symbol symbol : frame.getSymbols()) {
                json.name(symbol.getPrintName());
                frame.getVariable(symbol).accept(this);
            }
            json.endObject();
            super.visit(environment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    
    private void writeLogical(LogicalVector vector) throws IOException {
        for (int i = 0; i < vector.length(); i++) {
            int x = vector.getElementAsRawLogical(i);
            if(IntVector.isNA(x)) {
                json.value("NA");
            } else {
                json.value(x != 0);
            }
        }
    }
    
    private void writeIntVector(IntVector vector) throws IOException {
        for (int i = 0; i < vector.length(); ++i) {
            int x = vector.getElementAsInt(i);
            if(IntVector.isNA(x)) {
                json.value("NA");
            } else {
                json.value(x);
            }
        }
    }

    private void writeFactor(AtomicVector vector) throws IOException {
        StringVector levels = (StringVector) vector.getAttributes().get(Symbols.LEVELS);
        for (int i = 0; i < vector.length(); ++i) {
            if(vector.isElementNA(i)) {
                json.value("NA");
            } else {
                int levelIndex = vector.getElementAsInt(i);
                String level = levels.getElementAsString(levelIndex - 1);
                json.value(level);
            }
        }
    }


    private void writeDouble(DoubleVector vector) throws IOException {
        for (int i = 0; i < vector.length(); ++i) {
            double x = vector.getElementAsDouble(i);
            if(DoubleVector.isNA(x)) {
                json.value("NA");
            } else if(Double.isNaN(x)) {
                json.value("NaN");
            } else if(Double.isInfinite(x)) {
                if (x < 0) {
                    json.value("-Inf");
                } else {
                    json.value("Inf");
                }
            } else {
                int integer = (int)x;
                if(x == integer) {
                    json.value(integer);
                } else {
                    json.value(x);
                }
            }
        }
    }
    
    private void writeStringVector(StringVector vector) throws IOException {
        for (int i = 0; i < vector.length(); ++i) {
            String x = vector.getElementAsString(i);
            if(x == null) {
                json.value("NA");
            } else {
                json.value(x);
            }
        }
    }


    @Override
    public String getResult() {
        return writer.toString();
    }

}
