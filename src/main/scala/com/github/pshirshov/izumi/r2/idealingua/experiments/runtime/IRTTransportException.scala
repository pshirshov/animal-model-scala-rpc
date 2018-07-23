package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

trait IRTTransportException

class IRTUnparseableDataException(message: String) extends RuntimeException(message) with IRTTransportException

class IRTTypeMismatchException(message: String, val v: Any) extends RuntimeException(message) with IRTTransportException

class IRTMultiplexingException(message: String, val v: Any) extends RuntimeException(message) with IRTTransportException
