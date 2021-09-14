package overflowdb.codegen

import java.io.File
import overflowdb.schema.Schema
import scopt.OParser

object Main extends App {
  import Main2._

  val builder = OParser.builder[Config]
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("codegen"),
      opt[String]('c', "classWithSchema")
        .required()
        .action((x, c) => c.copy(classWithSchema = x))
        .text("class with schema field, e.g. `org.example.MyDomain$`"),
      opt[String]('f', "field")
        .required()
        .action((x, c) => c.copy(fieldName = x))
        .text("(static) field name for schema within the specified `classWithSchema` with schema field, e.g. `org.example.MyDomain$`"),
      opt[File]('o', "out")
        .required()
        .action((x, c) => c.copy(outputDir = x))
        .text("output directory"),
    )
  }

  OParser.parse(parser1, args, Config("", "", null)).foreach(execute)

}

object Main2 {
  case class Config(classWithSchema: String, fieldName: String, outputDir: File)

  def execute(config: Config): Seq[File] = config match {
    case Config(classWithSchema, fieldName, outputDir) =>
      val classLoader = getClass.getClassLoader
      val clazz = classLoader.loadClass(classWithSchema)
      val field = clazz.getDeclaredField(fieldName)
      assert(field.getType == classOf[Schema], s"field $fieldName in class `$classWithSchema` must be of type `overflowdb.schema.Schema`, but actually is of type `${field.getType}`")
      field.setAccessible(true)
      val schema = field.get(clazz).asInstanceOf[Schema]

      new CodeGen(schema).run(outputDir)
  }

}
