MAINFILE = main
MAKEDIR = directory
NASA = nasa
ROVER = rover

$(ROVER):
	@echo "Executing program..."
	@cd byteCode && java Main $(id) $(receiver) $(listen) $(multicastIP) $(action)

$(NASA): $(MAKEDIR)
	@echo "Compiling program..."
	@echo "Setting up a reliable data transport protocol...."
	@echo "Initializing rover, sender module and receiver modules..."
	@echo "---------------------------------------------------------"
	@javac -d byteCode *.java

clean:
	@rm -rf byteCode

$(MAKEDIR): $(MAINFILE) clean
	@echo "Creating byteCode"
	@mkdir -p byteCode

$(MAINFILE): Main.java