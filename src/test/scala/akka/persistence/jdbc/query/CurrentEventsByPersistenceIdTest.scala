/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.persistence.jdbc.query

import akka.persistence.query.{ EventEnvelope, Sequence }

abstract class CurrentEventsByPersistenceIdTest(config: String) extends QueryTestSpec(config) {

  it should "not find any events for unknown pid" in withActorSystem { implicit system =>
    val journalOps = new ScalaJdbcReadJournalOperations(system)
    journalOps.withCurrentEventsByPersistenceId()("unkown-pid", 0L, Long.MaxValue) { tp =>
      tp.request(Int.MaxValue)
      tp.expectComplete()
    }
  }

  it should "find events from an offset" in withActorSystem { implicit system =>
    val journalOps = new ScalaJdbcReadJournalOperations(system)
    withTestActors() { (actor1, actor2, actor3) =>
      actor1 ! 1
      actor1 ! 2
      actor1 ! 3
      actor1 ! 4

      eventually {
        journalOps.countJournal.futureValue shouldBe 4
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 0, 1) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(1), "my-1", 1, 1))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 1, 1) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(1), "my-1", 1, 1))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 1, 2) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(1), "my-1", 1, 1))
        tp.expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 2, 2) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 2, 3) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
        tp.expectNext(EventEnvelope(Sequence(3), "my-1", 3, 3))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 3, 3) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(3), "my-1", 3, 3))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 0, 3) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(1), "my-1", 1, 1))
        tp.expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
        tp.expectNext(EventEnvelope(Sequence(3), "my-1", 3, 3))
        tp.expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 1, 3) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(Sequence(1), "my-1", 1, 1))
        tp.expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
        tp.expectNext(EventEnvelope(Sequence(3), "my-1", 3, 3))
        tp.expectComplete()
      }
    }
  }

  it should "find events for actors" in withActorSystem { implicit system =>
    val journalOps = new JavaDslJdbcReadJournalOperations(system)
    withTestActors() { (actor1, actor2, actor3) =>
      actor1 ! 1
      actor1 ! 2
      actor1 ! 3

      eventually {
        journalOps.countJournal.futureValue shouldBe 3
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 1, 1) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, 1))
          .expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 2, 2) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
          .expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 3, 3) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, 3))
          .expectComplete()
      }

      journalOps.withCurrentEventsByPersistenceId()("my-1", 2, 3) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, 2))
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, 3))
          .expectComplete()
      }
    }
  }
}

class PostgresScalaCurrentEventsByPersistenceIdTest extends CurrentEventsByPersistenceIdTest("postgres-application.conf") with PostgresCleaner

class MySQLScalaCurrentEventsByPersistenceIdTest extends CurrentEventsByPersistenceIdTest("mysql-application.conf") with MysqlCleaner

class OracleScalaCurrentEventsByPersistenceIdTest extends CurrentEventsByPersistenceIdTest("oracle-application.conf") with OracleCleaner

class H2ScalaCurrentEventsByPersistenceIdTest extends CurrentEventsByPersistenceIdTest("h2-application.conf") with H2Cleaner
