package controllers
package actions
package resources

import forms._

import models.Truthy
import util.IpmiCommand
import util.concurrent.BackgroundProcessor
import util.plugins.{IpmiPowerCommand, PowerManagement}
import util.security.SecuritySpecification

import collins.power.Identify
import collins.power.management.{PowerManagement, PowerManagementConfig}

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.AsyncResult

case class EditAction(
  assettag: String,
  spec: SecuritySpecification,
  handler: SecureController
) extends SecureAction(spec, handler)  {



  override def validate(): Either[RequestDataHolder,RequestDataHolder] =  Right(EphemeralDataHolder())

  override def execute(rd: RequestDataHolder) = {
      val myasset = models.Asset.findByTag(assettag).get
      val display = myasset.getAllAttributes.exposeCredentials(user.canSeePasswords)
      Status.Ok(
        views.html.asset.edit(display,user )(flash, request)
      ) 
  }

 }
