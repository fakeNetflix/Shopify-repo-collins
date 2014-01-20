package controllers

import models.{Asset, AssetMeta, AssetMetaValue, MetaWrapper}
import util.config.Feature

import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import play.api.http.{Status => StatusValues}
import play.api.libs.json.{JsBoolean, JsObject}
import play.api.mvc.Results
import java.sql.SQLException


trait TagApi {
  this: Api with SecureController =>


  case class TagForm(name: String,  value: String, description: String) {
    def merge(asset: Asset) =  {
      MetaWrapper.createMeta(asset,Map((name,value)),description=Some(description))
    }
  }
  val TAG_FORM = Form(
    mapping(
      "name" -> text,
      "value" -> text,
      "description" -> text
    )(TagForm.apply)(TagForm.unapply)
  )

  def updateTagValues(tag: String) = SecureAction { implicit req =>
    Api.withAssetFromTag(tag) { asset =>
      TAG_FORM.bindFromRequest.fold(
        hasErrors => {
          val error = hasErrors.errors.map { _.message }.mkString(", ")
          Left(Api.getErrorMessage("Data submission error: %s".format(error)))
        },
        tagForm => {
          try {
            tagForm.merge(asset)
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
  }(Permissions.TagApi.UpdateTag)




  def getTags = SecureAction { implicit req =>
    val js = AssetMeta.findAll().sortBy(_.name).map { am =>
      val fields = Seq(
        ("name" -> JsString(am.name)),
        ("label" -> JsString(am.label)),
        ("description" -> JsString(am.description))
      )
      JsObject(fields)
    }
    val jsArray = JsArray(js.toList)
    val data = ResponseData(Results.Ok, JsObject(Seq("tags" -> jsArray)))
    formatResponseData(data)
  }(Permissions.TagApi.GetTags)

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



  }
