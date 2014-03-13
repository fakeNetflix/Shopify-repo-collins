package models

import play.api.libs.json._
import play.api.Logger

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{Schema, Table}
import collins.solr._


                //<th>@c.child_label</th><th><a href=@{"/asset/"+c.child_tag+"#hierarchy"}>@c.child_tag</a></th></tr>
case class HierarchyTableRow(
  end_index: Int,
  info: Option[HierarchyInfo],
  info_span: Int
)

case class HierarchyNode(
  asset_info: List[HierarchyInfo],
  parent: Option[Asset]
){

  def getDisplayTable(): List[HierarchyTableRow] = {
    var tableRows = List[HierarchyTableRow]()

    var ru_count = getRuCount()
    if (!ru_count.isEmpty)
    {

      for (i<- 1 to ru_count.get)
      {
        var asset = asset_info.find(_.child_end == i)

        val row = asset match{
          case Some(asset) => { HierarchyTableRow(i, Some(asset), (asset.child_end - asset.child_start + 1))}
          case None => { HierarchyTableRow(i, None, 1 )}
        }
        tableRows ::= row
      }
    }
    else
    {
      for( asset <- asset_info){
        val row  = HierarchyTableRow(-1, Some(asset), 1)
        tableRows ::= row
      }

    }

    tableRows
  }


  def getRuCount(): Option[Int] = {

    if(asset_info.length >0){

        val asset =  Asset.findByTag(asset_info.head.asset_tag)

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
    asset_tag: String,
    child_tag: String,
    child_label: String,
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
      "ASSET" -> JsString(asset_tag),
      "PRIORITY" -> JsNumber(priority),
      "CHILD_LABEL" -> JsString(child_label),
      "CHILD_TAG" -> JsString(child_tag),
      "CHILD_START" -> JsNumber(priority),
      "CHILD_END" -> JsNumber(priority)
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
  def createOrUpdate(asset: Asset, child_tag: String, child_label: Option[String] = None,  child_start: Option[Int] = None, child_end: Option[Int] = None) =
  {
    val existing = findLink(asset.tag, child_tag).getOrElse{
      inTransaction
      {
        create(HierarchyInfo(
            asset_tag = asset.tag,
            child_tag = child_tag,
            child_label = child_label.getOrElse(child_tag),
            child_start = child_start.getOrElse(-1),
            child_end = child_end.getOrElse(-1),
            priority = -1
          )
        )
      }
    }

  }



  def deleteLink(tag: String, child_tag: String) =
  {
    inTransaction {
       val results = tableDef.deleteWhere { p =>
         (p.asset_tag === tag) and
         (p.child_tag === child_tag)
       }
    }
  }
  def getHierarchyNode(asset: Asset):  Option[HierarchyNode] =
  {
      val child_info = inTransaction {
        from(tableDef)(a =>
          where(a.asset_tag === asset.tag)
          select(a)
        ).toList
      }

      val parent_tag =  findParent(asset.tag)

      val parent = parent_tag match {
        case Some(parent_tag) =>  Asset.findByTag(parent_tag)
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

  def findLink(assetTag: String, childTag: String): Option[HierarchyInfo] = {
    //getOrElseUpdate("HierarchyInfo.findByName(%s)".format(name.toUpperCase)) {
      inTransaction {
        tableDef.where(a =>
          a.asset_tag === assetTag and
          a.child_tag === childTag
        ).headOption
    }
  }

  def findChildren(assetTag: String): List[String] = {
    //getOrElseUpdate("HierarchyInfo.findByName(%s)".format(name.toUpperCase)) {
      inTransaction {
        from(tableDef)(a =>
          where(a.asset_tag === assetTag)
          select(a.child_tag)
        ).toList
    }
  }

  def findParent(childTag: String): Option[String] = {
    //getOrElseUpdate("HierarchyInfo.findByName(%s)".format(name.toUpperCase)) {
      inTransaction {
        from(tableDef)(a =>
          where(a.child_tag === childTag)
          select(a.asset_tag)
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
