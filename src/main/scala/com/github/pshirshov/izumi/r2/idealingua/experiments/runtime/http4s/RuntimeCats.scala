package com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.http4s

import cats.Monad
import cats.effect.IO
import com.github.pshirshov.izumi.r2.idealingua.experiments.runtime.ServiceResult

trait RuntimeCats {

  implicit object IOResult extends ServiceResult[IO] {
    override def map[A, B](r: IO[A])(f: A => B): IO[B] = implicitly[Monad[IO]].map(r)(f)

    override def flatMap[A, B](r: IO[A])(f: A => IO[B]): IO[B] = implicitly[Monad[IO]].flatMap(r)(f)

    override def pure[A](v: => A): IO[A] = implicitly[Monad[IO]].pure(v)
  }

}

object RuntimeCats extends RuntimeCats {

}
