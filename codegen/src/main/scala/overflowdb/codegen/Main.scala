package overflowdb.codegen

import overflowdb.schema.Schema

import java.io.File

object Main extends App {
  val outputDir =
    args.headOption.map(new File(_)).getOrElse(throw new AssertionError("please pass outputDir as first parameter"))

  val schemaStaticClass = "io.shiftleft.codepropertygraph.schema.CpgSchema$"
  val fieldName = "instance"
  val clazz = getClass.getClassLoader.loadClass(schemaStaticClass)
  val field = clazz.getDeclaredField(fieldName)
  assert(field.getType == classOf[Schema], s"field $fieldName in class `$schemaStaticClass` must be of type `overflowdb.schema.Schema`, but actually is of type `${field.getType}`")
  field.setAccessible(true)
  val schema = field.get(clazz).asInstanceOf[Schema]
  println(schema)
//  val schema = field.get().asInstanceOf[Schema]

//  new CodeGen(schema).run(outputDir)
}
