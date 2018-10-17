library(rjson)
library(hamcrest)

test.types <- function() {

    assertThat(toJSON(NULL), equalTo('null'))
    assertThat(toJSON(42), equalTo('42'))
    assertThat(toJSON(42L), equalTo('42'))
    assertThat(toJSON(TRUE), equalTo('true'))
    assertThat(toJSON(FALSE), equalTo('false'))
    assertThat(toJSON(NA), equalTo('"NA"'))
    assertThat(toJSON(integer(0)), equalTo('[]'))

    assertThat(toJSON(c(41,42)), equalTo('[41,42]'))
    assertThat(toJSON(c(41L,42L)), equalTo('[41,42]'))
    
    assertThat(toJSON(letters[1]), equalTo('"a"'))
    assertThat(toJSON(letters[1:4]), equalTo('["a","b","c","d"]'));
   
    assertThat(toJSON(list(1, 'a', TRUE, NULL)), equalTo('[1,"a",true,null]'))
}

test.factors <- function() {
    assertThat(toJSON(factor(c("yes", "no", "no", "yes"))), equalTo('["yes","no","no","yes"]'))
}

test.objects <- function() {
    assertThat(toJSON(c(a=1,b=2,c=3)), equalTo('{"a":1,"b":2,"c":3}'))
    assertThat(toJSON(c(a=1L,b=2L,c=3L)), equalTo('{"a":1,"b":2,"c":3}'))
    assertThat(toJSON(c(a=1L,b=2L,c=3L)), equalTo('{"a":1,"b":2,"c":3}'))
    assertThat(toJSON(list(a=1L,b=2L,c=3L)), equalTo('{"a":1,"b":2,"c":3}'))
    assertThat(toJSON(list(a=1L,b='foo',c=TRUE)), equalTo('{"a":1,"b":"foo","c":true}'))
}

test.readScalars <- function() {
    
    assertThat(fromJSON('null'), identicalTo(NULL));
    assertThat(fromJSON('true'), identicalTo(TRUE))
    assertThat(fromJSON('false'), identicalTo(FALSE))
    assertThat(fromJSON('false'), identicalTo(FALSE))
    assertThat(fromJSON('41'), identicalTo(41))
    assertThat(fromJSON('"foobar"'), identicalTo("foobar"))
}

test.readAtomicArrays <- function() {
    assertThat(fromJSON('[true,true,false]'), identicalTo(c(TRUE, TRUE, FALSE)))
    assertThat(fromJSON('[1,2,3,41,42]'), identicalTo(c(1,2,3,41, 42)))
    assertThat(fromJSON('["a","b","c"]'), identicalTo(letters[1:3]))
    assertThat(fromJSON('[1,"a",true]'), identicalTo(list(1,"a",TRUE)))
    assertThat(fromJSON('[]'), identicalTo(list()))
    assertThat(fromJSON('["NA",1]'), identicalTo(list("NA", 1)))
    assertThat(fromJSON('[null]'), identicalTo(list(NULL)))
}

test.readLargeStringArray <- function() {
    x <- rep(letters, times = 1000)
    assertThat(fromJSON(toJSON(x)), identicalTo(x))
}

test.readLargeNumberArray <- function() {
    x <- as.double(1:10000)
    y <- fromJSON(toJSON(x))
    assertThat(fromJSON(toJSON(x)), identicalTo(x))
}

test.readLargeLogicalArray <- function() {
    x <- rep(c(TRUE, TRUE, FALSE, TRUE), times = 10000)
    assertThat(fromJSON(toJSON(x)), identicalTo(x))
}

test.readNestedLists <- function() {
    assertThat(fromJSON('{"x":[1,2,3],"y":["a","b","c"]}'), identicalTo(list(x = c(1,2,3), y=letters[1:3])))
}

test.readObjects <- function() {
    assertThat(fromJSON('{"x": null, "y": 42}'), identicalTo(list(x = NULL, y = 42)))
    assertThat(fromJSON('{"x": { "a": 42, "b":  43}, "y": 42}'), identicalTo(list(x = list(a=42, b=43), y = 42)))
}

test.list3 <- function() {

    json <- fromJSON(file="list.json")
    assertThat(length(json), equalTo(3))
}

test.emptyArray <- function() {
    assertThat(fromJSON('{"x": null, "y": [] }'), identicalTo(list(x=NULL,y=list())))
}

test.github <- function() {
    json <- fromJSON(file="github.json")

}