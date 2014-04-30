package models.asset

import models.{Asset, Status, MetaWrapper}
import conversions._
import play.api.libs.json._

/**
 * An asset controlled by another collins instance, used during multi-collins
 * searching
 */
case class DetailedRemoteAsset(hostTag: String, remoteUrl: String, json: JsObject)
  extends RemoteAssetProxy((json \ "ASSET"))
{
  def getHostnameMetaValue() = (json \ "ATTRIBS" \ "0" \ "HOSTNAME").asOpt[String]
  def getPrimaryRoleMetaValue() = (json \ "ATTRIBS" \ "0" \ "PRIMARY_ROLE").asOpt[String]

  private[this] def warnAboutMetaData(): Option[MetaWrapper] = {
    None
  }


  def getMetaAttribute(name: String) = warnAboutMetaData()

  override def toJsValue = json ++ JsObject(Seq("LOCATION" -> JsString(hostTag)))
   
}
