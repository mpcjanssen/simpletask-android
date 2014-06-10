git fast-export master | fossil import --incremental .repo

fossil push http://mpcjanssen@mpcjanssen.nl/fossil/simpletask
 -R .repo