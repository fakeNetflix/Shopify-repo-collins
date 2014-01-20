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

case class BrowseAction(
  spec: SecuritySpecification,
  handler: SecureController
) extends SecureAction(spec, handler)  {



  override def validate(): Either[RequestDataHolder,RequestDataHolder] =  Right(EphemeralDataHolder())

  override def execute(rd: RequestDataHolder) = {
      Status.Ok(
        views.html.resources.browse(user )(flash, request)
      ) 
  }

 }
