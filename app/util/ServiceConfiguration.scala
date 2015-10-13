package util

import com.typesafe.config.ConfigFactory

object ServiceConfiguration {
  val baseRelativePath = "service.conf"
  val config = ConfigFactory.load(baseRelativePath)

  val rootUserScriptCatalog = config.getString("service.root.script.catalog")
  val rootUserOutputCatalog = config.getString("service.root.output.catalog")
  val rootUserFilesCatalog = config.getString("service.root.files.catalog")
}
