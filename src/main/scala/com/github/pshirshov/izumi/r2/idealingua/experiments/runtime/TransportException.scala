package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

trait TransportException

class UnparseableDataException(message: String) extends RuntimeException(message) with TransportException

class TypeMismatchException(message: String, val v: Any) extends RuntimeException(message) with TransportException

class MultiplexingException(message: String, val v: Any) extends RuntimeException(message) with TransportException
