package util
package power

import models.AssetMeta

/** A power related component (distribution unit, strip, outlet, etc) */
sealed trait PowerComponent extends Ordered[PowerComponent] {
  def componentType: Symbol
  def config: PowerConfiguration
  def id: Int // the id of the power unit
  // the position of the component within a unit, not physical position, this has to do with ordering during display
  def position: Int 

  def label = PowerConfiguration.Messages.ComponentLabel(typeName, sid)
  def meta: AssetMeta = AssetMeta.findOrCreateFromName(identifier)

  def missingData = PowerConfiguration.Messages.MissingData(key, label)

  final def identifier: String = "POWER_%s".format(typeName)
  final def isRequired: Boolean = true
  final def isUnique: Boolean = config.uniqueComponents.contains(componentType)
  final def key: String = "%s_%s".format(identifier, sid)
  final def sid: String = config.useAlphabeticNames match {
    case true => (65 + id).toChar.toString
    case false => id.toString
  }

  override def compare(that: PowerComponent) = (this.id - that.id) match {
    case 0 => this.position compare that.position
    case n => n
  }
  override def equals(o: Any) = o match {
    case that: PowerComponent =>
      this.id == that.id && this.componentType == that.componentType
    case _ =>
      false
  }
  override def hashCode = id.hashCode + componentType.hashCode

  protected[power] def typeName: String = componentType.name
}

case class PowerComponentValue(
  componentType: Symbol, config: PowerConfiguration, id: Int, position: Int
) extends PowerComponent
