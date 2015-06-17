package edu.isi.bmkeg.lapdf.extraction.exceptions;

public class ClassificationException extends Exception {

	public ClassificationException(String message) {
		super(message);
	}

	public ClassificationException() {
	}

	public ClassificationException(Throwable throwable) {
		super(throwable);
	}

	public ClassificationException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ClassificationException(String message, Throwable throwable, boolean enableSuppression, boolean writableStackTrace) {
		super(message, throwable, enableSuppression, writableStackTrace);
	}


}
