ARGS ?= 1280 720 video/Pencil_Candle_1280x720.mp4

run:
	mvn clean javafx:run -Dapp.arg="$(ARGS)"