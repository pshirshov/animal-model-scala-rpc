package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime

import scala.language.higherKinds


trait WithResultType[R[_]] {
  type Result[T] = R[T]
}

trait WithResult[R[_]] extends WithResultType[R] {
  protected def _ServiceResult: IRTServiceResult[R]

  protected def _Result[T](value: => T): R[T] = _ServiceResult.wrap(value)
}

trait WithContext[C] {
  type Context = C
}
