toJSON <- function( x, method = "C" ) {

	JsonWritingVisitor$toJson(x)

}

		
fromJSON <- function( json_str, file, method = "C", unexpected.escape = "error" )
{
	if( missing( json_str ) ) {
		if( missing( file ) )
			stop( "either json_str or file must be supplied to fromJSON")
		json_str <- paste(readLines( file, warn=FALSE ),collapse="")
	} else {
		if( missing( file ) == FALSE ) {
			stop( "only one of json_str or file must be supplied to fromJSON")
		}
	}
	return( JsonParser$parse(json_str) )
}





#create an object, which can be used to parse JSON data spanning multiple buffers
#it will be able to pull out multiple objects.. e.g: "[5][2,1]" is two different JSON objects - it can be called twice to get both items
newJSONParser <- function( method = "R" )
{
	if( method == "R" ) {
		buffer <- c()
		return(	list(
			"addData" = function( buf ) { 
				chars = strsplit(buf, "")[[1]]
				for( ch in chars )
					buffer[ length(buffer) + 1 ]  <<- ch
			},
			"getObject" = function()
			{
				tmp <- .parseValue( buffer, 1)
				if( is.null( tmp$incomplete ) == FALSE )
					return( NULL )

				if( tmp$size > length(buffer) )
					buffer <<- c()
				else
					buffer <<- buffer[ tmp$size : length(buffer) ]

				return( tmp$val )
			}
		) )
	} else if( method == "C" ) {
		buffer <- ""
		return(	list(
			"addData" = function( buf ) { 
				buffer <<- paste( buffer, buf, sep="" )
			},
			"getObject" = function()
			{
				tmp <- .Call("fromJSON", buffer, PACKAGE="rjson")
				if( any( class( tmp[[ 1 ]] ) == "incomplete" ) )
					return( NULL )

				size <- tmp[[ 2 ]] + 1

				buffer <<- substring( buffer, size, nchar( buffer ) )
				return( tmp[[ 1 ]] )
			}
		) )
	}
	stop("bad method - only R or C" )
}

