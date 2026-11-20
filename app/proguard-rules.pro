# R8 rules for the release builds, on top of the defaults (proguard-android-optimize.txt) and
# the rules the libraries bring.
#
# No class, field or method needs keeping here: nothing in Per Ankh finds code by name.
# Progress and reports are read and written with their generated serializers
# (Progress.serializer()), enums by their names, and the activity and the job service are kept by
# the rules generated from the manifest. A broad -keep would lower the share of code R8 may
# optimise and rename, which Google Play measures. Before adding one, check that the code really
# is reached by reflection.

# Line numbers in stack traces (ANRs, crashes), for reading them back with retrace and the
# build's mapping.txt. The source file is named "SourceFile" in every class.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
