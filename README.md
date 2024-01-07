# avdl-converter

A library for converting avdl files to avsc files automatically.

Useful for cases, when automatic scheme deployment is forbidden (in production) and avsc files are needed, but at the same time avdl language is preferred for describing avro schemes.
The library is converting all avdl files to avsc files during maven compile stage automatically. Gitlab-ci is given as an example for it to happen automatically on commit. For example, an avdl file is changed, then a pipeline runs compile stage, generates avsc files and commits them in the same branch.

## Customization
It is possible to customize generation of avsc files using Schema Resolvers. There are two options available: include only the first record from avdl file, or include all records marked with "#generated" comment.