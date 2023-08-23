[![Build Status](https://github.com/ShiftleftSecurity/overflowdb-codegen/workflows/release/badge.svg)](https://github.com/ShiftleftSecurity/overflowdb-codegen/actions?query=workflow%3Arelease)

# overflowdb-codegen

## generateDomainClasses
Freshly generates the domain classes to the configured location. If both the schema as well as the dependency tree (which includes the codegen plugin) are unchanged, it will not run. This is to avoid long build times due to unnecessary regeneration and recompilation for consecutive `sbt compile` runs. 

Note: the regular `sbt clean` does not touch/delete/handle the codegen-generated sources. This is on purpose, since the idea is to have the generated domain classes committed to the repository. 

## generateDomainClassesCheck
Fails if domain classes are not generated with the latest versions. Analogous to `scalafmtCheck`, i.e. run this on PRs.

## Example repositories / builds
* TODO

## Disable temporarily
You can temporarily disable the codegen in your build by setting the environment variable `ODB_CODEGEN_DISABLE=true`. That's useful e.g. if you made some manual changes to the generated files that would otherwise be overridden by the codegen. 
