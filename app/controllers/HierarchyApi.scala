package controllers

import models.{Asset,HierarchyInfo}
import util.config.Feature

import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import play.api.http.{Status => StatusValues}
import play.api.libs.json.{JsBoolean, JsObject}
import play.api.mvc.Results
import java.sql.SQLException


trait HierarchyApi {
  this: Api with SecureController =>


  case class HierarchyForm( child_tag: String, child_label: String, start: Option[Int] = None, end: Option[Int]) {
    def merge(asset: Asset) =  {
      HierarchyInfo.createOrUpdate(asset,child_tag,child_label=Some(child_label), child_start=start, child_end = end)
    }
  }
  val HIERARCHY_FORM = Form(
    mapping(
      "child" -> text,
      "label" -> text,
      "start" -> optional(number),
      "end" -> optional(number) 
    )(HierarchyForm.apply)(HierarchyForm.unapply)
  )

  def updateHierarchyValues(tag: String) = SecureAction { implicit req =>
    Api.withAssetFromTag(tag) { asset =>
      HIERARCHY_FORM.bindFromRequest.fold(
        hasErrors => {
          val error = hasErrors.errors.map { _.message }.mkString(", ")
          Left(Api.getErrorMessage("Data submission error: %s".format(error)))
        },
        hierarchyForm => {
          try {
            hierarchyForm.merge(asset)
            Right(ResponseData(Results.Ok, JsObject(Seq("SUCCESS" -> JsBoolean(true)))))
          } catch {
            case e: SQLException =>
              Left(Api.getErrorMessage("Erorr updating tag",
                Results.Status(StatusValues.CONFLICT)))
            case e =>
              Left(Api.getErrorMessage("Incomplete form submission: %s".format(e.getMessage)))
          }
        }
      )
    }.fold(
      err => formatResponseData(err),
      suc => formatResponseData(suc)
    )
  }(Permissions.HierarchyApi.UpdateHierarchy)



  def deleteHierarchyLink(tag: String, child_tag: String ) = SecureAction { implicit req =>
    HierarchyInfo.deleteLink(tag, child_tag)
    val js = JsObject(Seq("SUCCESS" -> JsBoolean(true)))
    formatResponseData(ResponseData(Results.Ok, JsObject(Seq("SUCCESS" -> JsBoolean(true)))))
  }(Permissions.HierarchyApi.UpdateHierarchy)

  def getChildren(tag: String) = SecureAction { implicit req =>
    var children = HierarchyInfo.findChildren(tag)
    val js = JsObject(Seq("values" -> JsArray(children.map(JsString(_)))))
    formatResponseData(ResponseData(Results.Ok,js))
  }(Permissions.HierarchyApi.UpdateHierarchy)

  def getParent(tag: String) = SecureAction { implicit req =>
    var parent = HierarchyInfo.findParent(tag).getOrElse("None")
    val js = JsObject(Seq("values" -> JsString(parent)))
    formatResponseData(ResponseData(Results.Ok, js ))
  }(Permissions.HierarchyApi.UpdateHierarchy)
  
  def getAllNodes() = SecureAction { implicit req =>
    var nodes = HierarchyInfo.getAllNodes()
    val js = JsObject(Seq("values" -> JsArray(nodes.map(_.asJsonObj ))))
    formatResponseData(ResponseData(Results.Ok,js))
  }(Permissions.HierarchyApi.UpdateHierarchy)


/*
  def getTagValues(tag: String) = SecureAction { implicit req =>
    val response =
      AssetMeta.findByName(tag).map { m =>
        if (Feature.encryptedTags.map(_.name).contains(m.name)) {
          Api.getErrorMessage("Refusing to give backs values for %s".format(m.name))
        } else {
          val s: Set[String] = AssetMetaValue.findByMeta(m).sorted.toSet
          val js = JsObject(Seq("values" -> JsArray(s.toList.map(JsString(_)))))
          ResponseData(Results.Ok, js)
        }
      }.getOrElse(Api.getErrorMessage("Tag not found", Results.NotFound))
    formatResponseData(response)
  }(Permissions.TagApi.GetTagValues)

*/

  }
