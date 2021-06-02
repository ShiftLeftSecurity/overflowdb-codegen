import overflowdb.schema.SchemaBuilder

trait TestSchema {
  val builder = new SchemaBuilder(basePackage = getClass.getCanonicalName.toLowerCase)
  lazy val instance = builder.build
}

