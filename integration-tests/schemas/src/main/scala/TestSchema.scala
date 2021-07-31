import overflowdb.schema.SchemaBuilder

trait TestSchema {
  val builder = new SchemaBuilder(domainShortName = "TestSchema", basePackage = getClass.getCanonicalName.toLowerCase)
  lazy val instance = builder.build
}

