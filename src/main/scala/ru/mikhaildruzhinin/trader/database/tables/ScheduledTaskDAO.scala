package ru.mikhaildruzhinin.trader.database.tables

import ru.mikhaildruzhinin.trader.database.Models.ScheduledTaskModel
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.Instant

class ScheduledTaskDAO(val profile: JdbcProfile) {
  import profile.api._

  //noinspection ScalaWeakerAccess
  private class ScheduledTaskTable(tag: Tag) extends Table[ScheduledTaskModel](tag, Some("trader"), "scheduled_tasks") {

    def taskName: Rep[String] = column[String]("task_name")

    def taskInstance: Rep[String] = column[String]("task_instance")

    def taskData: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]](
      "task_data",
      O.SqlType("bytea")
    )

    def executionTime: Rep[Instant] = column[Instant]("execution_time")

    def picked: Rep[Boolean] = column[Boolean]("picked")

    def pickedBy: Rep[Option[String]] = column[Option[String]]("picked_by")

    def lastSuccess: Rep[Option[Instant]] = column[Option[Instant]]("last_success")

    def lastFailure: Rep[Option[Instant]] = column[Option[Instant]]("last_failure")

    def consecutiveFailures: Rep[Option[Int]] = column[Option[Int]]("consecutive_failures")

    def lastHeartbeat: Rep[Option[Instant]] = column[Option[Instant]]("last_heartbeat")

    def version: Rep[Long] = column[Long]("version")

    def pk = primaryKey("scheduled_tasks_pkey", (taskName, taskInstance))

    override def * : ProvenShape[ScheduledTaskModel] = (
      taskName,
      taskInstance,
      taskData,
      executionTime,
      picked,
      pickedBy,
      lastSuccess,
      lastFailure,
      consecutiveFailures,
      lastHeartbeat,
      version) <> (ScheduledTaskModel.tupled, ScheduledTaskModel.unapply)
  }

  private lazy val table: TableQuery[ScheduledTaskTable] = TableQuery[ScheduledTaskTable]

  def createIfNotExists: profile.ProfileAction[Unit, NoStream, Effect.Schema] = table.schema.createIfNotExists

  def createIndices: DBIOAction[Unit, NoStream, Effect] = profile.api.DBIO.seq(
    sqlu"create index if not exists execution_time_idx on trader.scheduled_tasks (execution_time)",
    sqlu"create index if not exists last_heartbeat_idx on trader.scheduled_tasks (last_heartbeat)"
  )
}
