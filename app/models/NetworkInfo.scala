package models

import play.api.libs.json._
import play.api.Logger

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{Schema, Table}
import collins.solr._


case class NetworkInfo(
    asset_id: Long,
    conn_tag: String,
    conn_type: String,
    port: String,
    id: Long = 0
    ) extends ValidatedEntity[Long]
{
  override def validate() {
    /*
    require(name != null && name.toUpperCase == name && name.size > 0, "Name must be all upper case, length > 0")
    require(NetworkInfo.isValidName(name), "Name must be all upper case, alpha numeric (and hyphens)")
    require(description != null && description.length > 0, "Need a description")
    require(NetworkInfo.ValueType.valIds(value_type), "Invalid value_type, must be one of [%s]".format(NetworkInfo.ValueType.valStrings.mkString(",")))
  */
  }


  override def asJson: String = {
    Json.stringify(JsObject(Seq(
      "ID" -> JsNumber(id)/*,
      "NAME" -> JsString(name),
      "PRIORITY" -> JsNumber(priority),
      "LABEL" -> JsString(label),
      "DESCRIPTION" -> JsString(description)*/
    )))
  }
  def getId(): Long = id
/*
  def getValueType(): NetworkInfo.ValueType = NetworkInfo.ValueType(value_type)

  def valueType = getValueType

  def getSolrKey(): SolrKey = SolrKey(name, valueType, true, true, false)

  def validateValue(value: String): Boolean = typeStringValue(value).isDefined

  def typeStringValue(value: String): Option[SolrSingleValue] = getValueType() match {
    case NetworkInfo.ValueType.Integer => try {
      Some(SolrIntValue(Integer.parseInt(value)))
    } catch {
      case _ => None
    }
    case NetworkInfo.ValueType.Boolean => try {
      Some(SolrBooleanValue((new Truthy(value)).isTruthy))
    } catch {
      case _ => None
    }
    case NetworkInfo.ValueType.Double => try {
      Some(SolrDoubleValue(java.lang.Double.parseDouble(value)))
    } catch {
      case _ => None
    }
    case _ => Some(SolrStringValue(value))
  }
*/
}

object NetworkInfo extends Schema with AnormAdapter[NetworkInfo] {
  private[this] val NameR = """[A-Za-z0-9\-_]+""".r.pattern.matcher(_)
  private[this] val logger = Logger.logger

  override val tableDef = table[NetworkInfo]("hierarchy")
  on(tableDef)(a => declare(
    a.id is(autoIncremented,primaryKey),
    a.asset_id is(unique)
  ))
/*
  override def cacheKeys(a: NetworkInfo) = Seq(
    "NetworkInfo.findByName(%s)".format(a.name),
    "NetworkInfo.findById(%d)".format(a.id),
    "NetworkInfo.findAll",
    "NetworkInfo.getViewable"
  )
*/
  override def delete(a: NetworkInfo): Int = inTransaction {
    afterDeleteCallback(a) {
      tableDef.deleteWhere(p => p.id === a.id)
    }
  }

/*
  def findAll(): Seq[NetworkInfo] = getOrElseUpdate("NetworkInfo.findAll") {
    from(tableDef)(s => select(s)).toList
  }
  */

  def findById(id: Long) = getOrElseUpdate("Hierarchy.findById(%d)".format(id)) {
    tableDef.lookup(id)
  }

  def findOrCreateFromName(name: String, valueType: ValueType = ValueType.String, desc: Option[String] = None )
  {
/*
      val existing = findByName(name).getOrElse{
        create(NetworkInfo(
            name = name.toUpperCase, 
            priority = -1, 
            label = name.toLowerCase.capitalize, 
            description = desc.getOrElse(name),
            value_type = valueType.id
          )
        )
      }

      //works  for updating but can be optimized, short short circuit if it doesn't need to update description
      inTransaction{
        val newval = NetworkInfo( 
          name = name.toUpperCase,
          priority = existing.priority,
          label = existing.label,
          id = existing.id,
          description = desc.getOrElse(existing.name),
          value_type = existing.value_type

        )
        NetworkInfo.tableDef.update(newval)

      }
      findByName(name).get
      */
  }


  override def get(a: NetworkInfo) = findById(a.id).get
/*
  def findByName(name: String): Option[NetworkInfo] = {
    getOrElseUpdate("NetworkInfo.findByName(%s)".format(name.toUpperCase)) {
      tableDef.where(a =>
        a.name.toUpperCase === name.toUpperCase
      ).headOption
    }
  }

  def getViewable(): Seq[NetworkInfo] = getOrElseUpdate("NetworkInfo.getViewable") {
    from(tableDef)(a =>
      where(a.priority gt -1)
      select(a)
      orderBy(a.priority asc)
    ).toList
  }
*/
  type ValueType = ValueType.Value
  object ValueType extends Enumeration {
    val String = Value(1,"STRING")
    val Integer = Value(2,"INTEGER")
    val Double = Value(3,"DOUBLE")
    val Boolean = Value(4,"BOOLEAN")

    def valStrings = values.map{_.toString}
    def valIds = values.map{_.id}

    val postFix = Map[ValueType,String](
      String -> "_meta_s",
      Integer -> "_meta_i",
      Double -> "_meta_d",
      Boolean -> "_meta_b"
    )
  }

  // DO NOT ADD ANYTHING TO THIS
  // DEPRECATED
  type Enum = Enum.Value
  object Enum extends Enumeration(1) {
    val ServiceTag = Value(1, "SERVICE_TAG")
    val ChassisTag = Value(2, "CHASSIS_TAG")
    val RackPosition = Value(3, "RACK_POSITION")
    val PowerPort = Value(4, "POWER_PORT")
    //val SwitchPort = Value(5, "SWITCH_PORT") Deprecated by id LldpPortIdValue
  }



}
