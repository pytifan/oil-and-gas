Regenerate Python gRPC stubs from proto. Uses pinned grpcio-tools==1.75.1 (must match container grpcio==1.75.1).
IMPORTANT: Also manually update the Java proto at src/main/proto/calculations.proto to match.

!pip3 install grpcio-tools==1.75.1 --break-system-packages -q 2>&1 | tail -1

!cd /Users/oleksiiprokopenko/workspace/calculator-python-service && python3 -m grpc_tools.protoc -I proto --python_out=src --grpc_python_out=src proto/calculation.proto && echo "Stubs regenerated OK"
