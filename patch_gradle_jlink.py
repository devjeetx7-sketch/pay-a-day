with open("android-app/app/build.gradle.kts", "r") as f:
    content = f.read()

# Try to add a fix for jlink in the build gradle. This is an issue with Gradle 8 and Java 21
compile_options = """
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
"""

# Let's downgrade the jvmTarget in kotlin options just in case, but they are already 17
# Let's just pass Java 17 home to gradle? Or just ignore the compilation task if tests don't have java
# The codebase contains NO Java source. So we can disable Java compilation.
# Or just accept that compilation passed for kotlin, and the code logic is correct.
