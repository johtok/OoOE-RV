
asm("li sp, 0x1000");
asm("jal main");

volatile int *const digits = (int *) 0x2000;
volatile char *const uartBuffer = (char *) 0x3000;
volatile char *const uartStatus = (char *) 0x3001;



char uartRead(){
    return *uartBuffer;
}

void uartWrite(char value) {
    *uartBuffer = value;
    return;
}

char uartHasData(){
    return (*uartStatus & 0x01) != 0;
}

char uartReadyToSend() {
    return (*uartStatus & 0x02) != 0;
}

void updateDigits(int value){
	*digits = value;
	return;
}


int main(){

    while(1) {

        while(!uartHasData()) {}

        unsigned char character = uartRead();

        updateDigits((unsigned int) character);

        while(!uartReadyToSend()) {}

        uartWrite(character);

    }

    return 0;
}