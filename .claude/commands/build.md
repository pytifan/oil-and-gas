Build the gateway JAR (skip tests):

!cd /Users/oleksiiprokopenko/workspace/calculations-gateway && ./mvnw package -DskipTests 2>&1 | grep -E "BUILD|ERROR|error" | tail -5
