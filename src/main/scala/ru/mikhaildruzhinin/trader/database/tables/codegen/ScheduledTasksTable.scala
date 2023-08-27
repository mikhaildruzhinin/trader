package ru.mikhaildruzhinin.trader.database.tables.codegen
// AUTO-GENERATED Slick data model for table ScheduledTasks
trait ScheduledTasksTable {

  self:Tables  =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}
  /** Entity class storing rows of table ScheduledTasks
   *  @param taskName Database column task_name SqlType(text)
   *  @param taskInstance Database column task_instance SqlType(text)
   *  @param taskData Database column task_data SqlType(bytea), Default(None)
   *  @param executionTime Database column execution_time SqlType(timestamptz)
   *  @param picked Database column picked SqlType(bool)
   *  @param pickedBy Database column picked_by SqlType(text), Default(None)
   *  @param lastSuccess Database column last_success SqlType(timestamptz), Default(None)
   *  @param lastFailure Database column last_failure SqlType(timestamptz), Default(None)
   *  @param consecutiveFailures Database column consecutive_failures SqlType(int4), Default(None)
   *  @param lastHeartbeat Database column last_heartbeat SqlType(timestamptz), Default(None)
   *  @param version Database column version SqlType(int8) */
  case class ScheduledTasksRow(taskName: String, taskInstance: String, taskData: Option[Array[Byte]] = None, executionTime: java.sql.Timestamp, picked: Boolean, pickedBy: Option[String] = None, lastSuccess: Option[java.sql.Timestamp] = None, lastFailure: Option[java.sql.Timestamp] = None, consecutiveFailures: Option[Int] = None, lastHeartbeat: Option[java.sql.Timestamp] = None, version: Long)
  /** GetResult implicit for fetching ScheduledTasksRow objects using plain SQL queries */
  implicit def GetResultScheduledTasksRow(implicit e0: GR[String], e1: GR[Option[Array[Byte]]], e2: GR[java.sql.Timestamp], e3: GR[Boolean], e4: GR[Option[String]], e5: GR[Option[java.sql.Timestamp]], e6: GR[Option[Int]], e7: GR[Long]): GR[ScheduledTasksRow] = GR{
    prs => import prs._
    ScheduledTasksRow.tupled((<<[String], <<[String], <<?[Array[Byte]], <<[java.sql.Timestamp], <<[Boolean], <<?[String], <<?[java.sql.Timestamp], <<?[java.sql.Timestamp], <<?[Int], <<?[java.sql.Timestamp], <<[Long]))
  }
  /** Table description of table scheduled_tasks. Objects of this class serve as prototypes for rows in queries. */
  class ScheduledTasks(_tableTag: Tag) extends profile.api.Table[ScheduledTasksRow](_tableTag, Some("trader"), "scheduled_tasks") {
    def * = (taskName, taskInstance, taskData, executionTime, picked, pickedBy, lastSuccess, lastFailure, consecutiveFailures, lastHeartbeat, version) <> (ScheduledTasksRow.tupled, ScheduledTasksRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(taskName), Rep.Some(taskInstance), taskData, Rep.Some(executionTime), Rep.Some(picked), pickedBy, lastSuccess, lastFailure, consecutiveFailures, lastHeartbeat, Rep.Some(version))).shaped.<>({r=>import r._; _1.map(_=> ScheduledTasksRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6, _7, _8, _9, _10, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column task_name SqlType(text) */
    val taskName: Rep[String] = column[String]("task_name")
    /** Database column task_instance SqlType(text) */
    val taskInstance: Rep[String] = column[String]("task_instance")
    /** Database column task_data SqlType(bytea), Default(None) */
    val taskData: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]]("task_data", O.Default(None))
    /** Database column execution_time SqlType(timestamptz) */
    val executionTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("execution_time")
    /** Database column picked SqlType(bool) */
    val picked: Rep[Boolean] = column[Boolean]("picked")
    /** Database column picked_by SqlType(text), Default(None) */
    val pickedBy: Rep[Option[String]] = column[Option[String]]("picked_by", O.Default(None))
    /** Database column last_success SqlType(timestamptz), Default(None) */
    val lastSuccess: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("last_success", O.Default(None))
    /** Database column last_failure SqlType(timestamptz), Default(None) */
    val lastFailure: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("last_failure", O.Default(None))
    /** Database column consecutive_failures SqlType(int4), Default(None) */
    val consecutiveFailures: Rep[Option[Int]] = column[Option[Int]]("consecutive_failures", O.Default(None))
    /** Database column last_heartbeat SqlType(timestamptz), Default(None) */
    val lastHeartbeat: Rep[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("last_heartbeat", O.Default(None))
    /** Database column version SqlType(int8) */
    val version: Rep[Long] = column[Long]("version")

    /** Primary key of ScheduledTasks (database name scheduled_tasks_pkey) */
    val pk = primaryKey("scheduled_tasks_pkey", (taskName, taskInstance))

    /** Index over (executionTime) (database name execution_time_idx) */
    val index1 = index("execution_time_idx", executionTime)
    /** Index over (lastHeartbeat) (database name last_heartbeat_idx) */
    val index2 = index("last_heartbeat_idx", lastHeartbeat)
  }
  /** Collection-like TableQuery object for table ScheduledTasks */
  lazy val ScheduledTasks = new TableQuery(tag => new ScheduledTasks(tag))
}
