package models

import play.api.libs.json._
import play.api.Logger

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{Schema, Table}
import collins.solr._

case class RackUnitInfo(
  maintenance_tag: String,
  hostname: Option[String],
  ip_address: Option[String],
  ipmi_address: Option[String],
  nodes: Option[List[RackUnitInfo]]

)

case class HierarchyTableRow(
  end_index: Int,
  asset_tag: Option[String],
  info: Option[HierarchyInfo],
  node_info: Option[RackUnitInfo],
  info_span: Int
)

case class HierarchyNode(
  asset_info: List[HierarchyInfo],
  parent: Option[Asset]
){


  def getServerNodeInfo( asset: Asset ): RackUnitInfo = {
    val aa = asset.getAllAttributes
    val hostname = aa.mvs.filter(_.getName() == "HOSTNAME") match {
        case Nil => "none"
        case x  => x.head.toString
    }

    val maint_tag = aa.mvs.filter(_.getName() == "MAINTENANCE_TAG") match {
        case Nil => "UNKNOWN"
        case x  => x.head.toString
    }

    val ips = aa.addresses.map(_.dottedAddress).toList.mkString(" , ")
    val ipmi = aa.ipmi.get.dottedAddress()
    RackUnitInfo(maint_tag, Some(hostname), Some(ips), Some(ipmi), None )
  }


  def getServerChassisInfo( asset: Asset ): RackUnitInfo = {
    var children = HierarchyInfo.findChildren(asset.id)
    var nodes = List[RackUnitInfo]()

    val aa = asset.getAllAttributes
    val maint_tag = aa.mvs.filter(_.getName() == "MAINTENANCE_TAG") match {
        case Nil => "UNKNOWN"
        case x  => x.head.toString
    }


    for( child <- children ){
      var asset = Asset.findById(child).get
      var node_info = getServerNodeInfo(asset)
      nodes ::= node_info
    }
    RackUnitInfo(maint_tag, None, None, None, Some(nodes) )
  }

  def getGenericInfo( asset: Asset): RackUnitInfo = {
    val aa = asset.getAllAttributes
    val hostname = aa.mvs.filter(_.getName() == "HOSTNAME") match {
        case Nil => ""
        case x  => x.head.toString
    }

    val maint_tag = aa.mvs.filter(_.getName() == "MAINTENANCE_TAG") match {
        case Nil => "UNKNOWN"
        case x  => x.head.toString
    }
    val asset_type = asset.getType().toString

    val ips = aa.addresses.map(_.dottedAddress).toList.mkString(" , ")
    RackUnitInfo(maint_tag, Some(hostname), Some(ips), None, None )

  }


  def getDisplayTable(): List[HierarchyTableRow] = {
    var tableRows = List[HierarchyTableRow]()

    var ru_count = getRuCount()
    if (!ru_count.isEmpty)
    {

      for (i<- 1 to ru_count.get)
      {
        var asset_data = asset_info.find(_.child_end == i)


        // Need to check if asset is server_node
        val row = asset_data match{
          case Some(asset_data) => {
            val asset = Asset.findById(asset_data.child_id).get
            val tag = Some(asset.tag)
            val span = (asset_data.child_end - asset_data.child_start + 1)

            val asset_type = asset.getType().toString

            val node_info = asset_type match {
              case "SERVER_NODE" => { Some(getServerNodeInfo(asset)) }
              case "SERVER_CHASSIS" => { Some(getServerChassisInfo(asset)) }
              case _ => { Some(getGenericInfo(asset)) }
              }

            HierarchyTableRow(i, tag, Some(asset_data), node_info, span )
          }
          case None => { HierarchyTableRow(i, None, None, None, 1 )}
        }
        tableRows ::= row
      }
    }
    else
    {
      for( asset_data <- asset_info){


        val asset = Asset.findById(asset_data.child_id).get
        val tag = Some(asset.tag)
        val asset_type = asset.getType().toString
                    
        val node_info = asset_type match {
          case "SERVER_NODE" => { Some(getServerNodeInfo(asset)) }
          case "SERVER_CHASSIS" => { Some(getServerChassisInfo(asset)) }
          case _ => { Some(getGenericInfo(asset)) }
        }

        val row = HierarchyTableRow(-1, tag, Some(asset_data), node_info,  1 )
        tableRows ::= row
      }

    }

    tableRows
  }


  def getRuCount(): Option[Int] = {

    if(asset_info.length >0){

        val asset =  Asset.findById(asset_info.head.asset_id)

        val attrs = asset.get.getAllAttributes
        val ru_check = attrs.mvs.find(_.getName == "RU_COUNT")
        ru_check match{
          case Some(_) => { Some(ru_check.get.getValue.toInt)}
          case None => { None }
        }
    }else{
      None
    }

  }
}

case class HierarchyInfo(
    asset_id: Long,
    child_id: Long,
    child_start: Int,
    child_end: Int,
    id: Long = 0,
    priority: Long
    ) extends ValidatedEntity[Long]
{
  override def validate() {
    /*
    require(name != null && name.toUpperCase == name && name.size > 0, "Name must be all upper case, length > 0")
    require(HierarchyInfo.isValidName(name), "Name must be all upper case, alpha numeric (and hyphens)")
    require(description != null && description.length > 0, "Need a description")
    require(HierarchyInfo.ValueType.valIds(value_type), "Invalid value_type, must be one of [%s]".format(HierarchyInfo.ValueType.valStrings.mkString(",")))
  */
  }


  override def asJson: String = {
    Json.stringify(asJsonObj)
  }

  def asJsonObj: JsObject = {
    JsObject(Seq(
      "ID" -> JsNumber(id),
      "ASSET" -> JsString(Asset.findById(asset_id).get.tag),
      "ASSET_ID" -> JsNumber(asset_id),
      "PRIORITY" -> JsNumber(priority),
      "CHILD_TAG" -> JsString(Asset.findById(child_id).get.tag),
      "CHILD_ID" -> JsNumber(child_id),
      "CHILD_START" -> JsNumber(child_start),
      "CHILD_END" -> JsNumber(child_end)
    ))
  }
  def getId(): Long = id
/*
  def getValueType(): HierarchyInfo.ValueType = HierarchyInfo.ValueType(value_type)

  def valueType = getValueType

  def getSolrKey(): SolrKey = SolrKey(name, valueType, true, true, false)

  def validateValue(value: String): Boolean = typeStringValue(value).isDefined

  def typeStringValue(value: String): Option[SolrSingleValue] = getValueType() match {
    case HierarchyInfo.ValueType.Integer => try {
      Some(SolrIntValue(Integer.parseInt(value)))
    } catch {
      case _ => None
    }
    case HierarchyInfo.ValueType.Boolean => try {
      Some(SolrBooleanValue((new Truthy(value)).isTruthy))
    } catch {
      case _ => None
    }
    case HierarchyInfo.ValueType.Double => try {
      Some(SolrDoubleValue(java.lang.Double.parseDouble(value)))
    } catch {
      case _ => None
    }
    case _ => Some(SolrStringValue(value))
  }
*/
}

object HierarchyInfo extends Schema with AnormAdapter[HierarchyInfo] {
  private[this] val NameR = """[A-Za-z0-9\-_]+""".r.pattern.matcher(_)
  private[this] val logger = Logger.logger

  override val tableDef = table[HierarchyInfo]("hierarchy")
  on(tableDef)(a => declare(
    a.id is(autoIncremented,primaryKey),
    a.priority is(indexed)
  ))
/*
  override def cacheKeys(a: HierarchyInfo) = Seq(
    "HierarchyInfo.findByName(%s)".format(a.name),
    "HierarchyInfo.findById(%d)".format(a.id),
    "HierarchyInfo.findAll",
    "HierarchyInfo.getViewable"
  )
*/
  override def delete(a: HierarchyInfo): Int = inTransaction {
    afterDeleteCallback(a) {
      tableDef.deleteWhere(p => p.id === a.id)
    }
  }

/*
  def findAll(): Seq[HierarchyInfo] = getOrElseUpdate("HierarchyInfo.findAll") {
    from(tableDef)(s => select(s)).toList
  }
  */

  def findById(id: Long) = getOrElseUpdate("Hierarchy.findById(%d)".format(id)) {
    tableDef.lookup(id)
  }


  // updaote not supported yet
  def createOrUpdate(asset: Asset, child_tag: String,  child_start: Option[Int] = None, child_end: Option[Int] = None) =
  {
   logger.debug( "Got tag %s".format(child_tag) )
    val child = Asset.findByTag(child_tag).get // this could be bad

    val existing = findLink(asset.id, child.id).getOrElse{
      inTransaction
      {
        create(HierarchyInfo(
            asset_id = asset.id,
            child_id = child.id,
            child_start = child_start.getOrElse(-1),
            child_end = child_end.getOrElse(-1),
            priority = -1
          )
        )
      }
    }

  }



  def deleteLink(id: Long, child_id: Long) =
  {
    inTransaction {
       val results = tableDef.deleteWhere { p =>
         (p.asset_id === id) and
         (p.child_id === child_id)
       }
    }
  }
  def getHierarchyNode(asset: Asset):  Option[HierarchyNode] =
  {
      val child_info = inTransaction {
        from(tableDef)(a =>
          where(a.asset_id === asset.id)
          select(a)
        ).toList
      }

      val parent_id =  findParent(asset.id)

      val parent = parent_id match {
        case Some(parent_id) =>  Asset.findById(parent_id)
        case None =>  None
      }


      var hierarchy = if ( child_info.isEmpty && parent.isEmpty )
      {
        None
      }
      else
      {
        Some(new HierarchyNode(child_info, parent))
      }
      hierarchy

  }

  override def get(a: HierarchyInfo) = findById(a.id).get

  def findLink(assetId: Long, childId: Long): Option[HierarchyInfo] = {
      inTransaction {
        tableDef.where(a =>
          a.asset_id === assetId and
          a.child_id === childId
        ).headOption
    }
  }

  def findChildren(assetId: Long): List[Long] = {
      inTransaction {
        from(tableDef)(a =>
          where(a.asset_id === assetId)
          select(a.child_id)
        ).toList
    }
  }

  def findParent(childId: Long): Option[Long] = {
    //getOrElseUpdate("HierarchyInfo.findByName(%s)".format(name.toUpperCase)) {
      inTransaction {
        from(tableDef)(a =>
          where(a.child_id === childId)
          select(a.asset_id)
        ).headOption
      }

  }

  def getAllNodes(): List[HierarchyInfo] = {
    //getOrElseUpdate("HierarchyInfo.findByName(%s)".format(name.toUpperCase)) {
       inTransaction {
        from(tableDef)(a =>
          select(a)
        ).toList
      }

  }

/*
  def getViewable(): Seq[HierarchyInfo] = getOrElseUpdate("HierarchyInfo.getViewable") {
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
