// defines for the frequency of the notes (.5 x freq of mid C)
#define C4 261
#define Db4 277
#define D4 293
#define Eb4 311
#define E4 329
#define F4 349
#define Gb4 369
#define G4 392
#define Ab4 415
#define A4 440
#define Bb4 466
#define B4 493
#define C5 523
#define Db5 554
#define D5 587
#define Eb5 622
#define E5 659
#define F5 698
#define Gb5 740
#define G5 784
#define Ab5 830
#define A5 880
#define Bb5 932
#define B5 987
#define C6 1046
#define Db6 1109
#define D6 1174
#define Eb6 1244
#define E6 1318
#define F6 1397
#define Gb6 1480
#define G6 1567
#define Ab6 1661
#define Bb6 1864
// defines for the duration of the notes (in ms)

#define bpm (130)
#define bt 240000/bpm

#define wh    bt
#define h      bt/2
//#define dq     448
#define q      h/2
//#define qt     170
//#define de     192
//#define e      128
//#define et      85
//#define oo7      1    // 007 jingle

#define thrd bt/3
#define sx bt/6
#define tlw bt/12
#define tw4 bt/24

void note(uint16_t n,uint16_t duration,uint16_t til_next_note) {
  tone(PIEZO_PIN, n);
  delay(duration);
  noTone(PIEZO_PIN);
  delay(til_next_note-duration);
}
