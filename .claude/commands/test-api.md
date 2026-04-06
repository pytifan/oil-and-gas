Submit a well completion calculation and stream the SSE progress events. Requires all services running.

!CALC_ID=$(curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -d '{"wellParams":{"tubingLengthM":4000,"tubingOdMm":89,"tubingWallMm":6.5,"casingOdMm":168,"casingWallMm":10,"fluidDensityKgM3":1020,"gravityMpS2":9.81,"initialWaterLevelM":0,"surfacePressurePa":100000,"maxWellheadPressurePa":20000000,"minWellheadPressurePa":10000000},"options":{"unitSystem":"metric"}}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['calculationId'])") \
  && echo "Calc ID: $CALC_ID" \
  && curl -sN --max-time 10 "http://localhost:8080/api/v1/calculations/$CALC_ID/progress" | head -20
