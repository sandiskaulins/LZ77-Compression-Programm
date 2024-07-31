
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;

public class Main {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		String choiseStr;
		String sourceFile, resultFile, firstFile, secondFile;
		
		loop: while (true) {
			
			choiseStr = sc.next();
								
			switch (choiseStr) {
			case "comp":
				System.out.print("source file name: ");
				sourceFile = sc.next();
				System.out.print("archive name: ");
				resultFile = sc.next();
				comp(sourceFile, resultFile);
				break;
			case "decomp":
				System.out.print("archive name: ");
				sourceFile = sc.next();
				System.out.print("file name: ");
				resultFile = sc.next();
				decomp(sourceFile, resultFile);
				break;
			case "size":
				System.out.print("file name: ");
				sourceFile = sc.next();
				size(sourceFile);
				break;
			case "equal":
				System.out.print("first file name: ");
				firstFile = sc.next();
				System.out.print("second file name: ");
				secondFile = sc.next();
				System.out.println(equal(firstFile, secondFile));
				break;
			case "about":
				about();
				break;
			case "exit":
				break loop;
			}
		}

		sc.close();
	}

	public static void comp(String sourceFile, String resultFile) {
		Lz77 lz77 = new Lz77();
        lz77.compress(sourceFile, resultFile);
        System.out.println("Compression completed.");
	}

	public static void decomp(String sourceFile, String resultFile) {
		Lz77 lz77 = new Lz77();
        lz77.decompress(sourceFile, resultFile);
        System.out.println("Decompression completed.");
	}
	
	public static void size(String sourceFile) {
		try {
			FileInputStream f = new FileInputStream(sourceFile);
			System.out.println("size: " + f.available());
			f.close();
		}
		catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		
	}
	
	public static boolean equal(String firstFile, String secondFile) {
		try {
			FileInputStream f1 = new FileInputStream(firstFile);
			FileInputStream f2 = new FileInputStream(secondFile);
			int k1, k2;
			byte[] buf1 = new byte[1000];
			byte[] buf2 = new byte[1000];
			do {
				k1 = f1.read(buf1);
				k2 = f2.read(buf2);
				if (k1 != k2) {
					f1.close();
					f2.close();
					return false;
				}
				for (int i=0; i<k1; i++) {
					if (buf1[i] != buf2[i]) {
						f1.close();
						f2.close();
						return false;
					}
						
				}
			} while (!(k1 == -1 && k2 == -1));
			f1.close();
			f2.close();
			return true;
		}
		catch (IOException ex) {
			System.out.println(ex.getMessage());
			return false;
		}
	}
	
	public static void about() {
		System.out.println("Sandis Kauliņš");
	}
}

class Lz77 {

    private static final int WINDOW_SIZE = 32768;
    private static final int LOOKAHEAD_BUFFER = 255;

    public void compress(String sourceFile, String resultFile) {
        try {
            // Datu nolasīšana no faila
            FileInputStream in = new FileInputStream(sourceFile);
            byte[] data = new byte[in.available()];
            in.read(data);
            in.close();

            // Saraksts, kurā glabāsies kompresētie tokeni <nobīde,garums,nākamais simbols>
            List<LZ77Token> compressedTokens = new LinkedList<>();
            int cursor = 0;

            //Datu kompresijas cikls, kas turpinās, līdz visi dati nav nokompresēti
            while (cursor < data.length) {

                int bestMatchLength = 0;
                int offset = 0;

                // Cikls, kas pārbauda sakritības ar iepriekšējiem datiem
                for (int i = Math.max(0, cursor - WINDOW_SIZE); i < cursor; i++) {
                    int length = 0;
                    // Cikls, kas meklē sakritības starp iepriekšējiem un pašreizējiem datiem
                    while (cursor + length < data.length // Pārbauda, vai cursor + length ir mazāks par kopējo datu garumu, lai nepārniegtu masīva robežas
                    && data[i + length] == data[cursor + length] // Pārbauda, vai dati, kas atrodas attiecīgajos pozīcijās i un cursor, ir vienādi
                    && length < LOOKAHEAD_BUFFER) {
                        length++;
                    }

                    // Ja šī sakritība ir garāka par pašreizējo, atjaunojam informāciju par labāko sakritību
                    if (length > bestMatchLength) {
                        bestMatchLength = length; // garuma atjaunošana
                        offset = cursor - i; // nobīdes atjaunošana
                    }
                }

                // Ja ir atrasta sakritība, pievienojam tokenu sarakstam
                if ((bestMatchLength > 0) & ((bestMatchLength+cursor) < data.length)) {
                    compressedTokens.add(new LZ77Token(offset, bestMatchLength, data[cursor + bestMatchLength]));
                    cursor += bestMatchLength + 1;
                } else {
                    compressedTokens.add(new LZ77Token(0, 0, data[cursor]));
                    cursor++;
                }
            }

            //Rezūltātu pierakstīšana failā

            FileOutputStream out = new FileOutputStream(resultFile);

            for (LZ77Token token : compressedTokens) {
                out.write((byte) (token.offset >> 8)); // Rakstam nobīdes pirmo (augšējo) baitu
                out.write((byte) token.offset); // Rakstam nobīdes otro (apakšējo) baitu
                out.write((byte) token.length); 
                out.write(token.nextChar);
            }

            out.close();
            
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void decompress(String sourceFile, String resultFile) {

        try {
            
            // Datu nolasīšana no faila
            FileInputStream in = new FileInputStream(sourceFile);
            byte[] data = new byte[in.available()];
            in.read(data);
            in.close();

            // Saraksts, kurā glabājas dekompresēta faila saturs
            List<Byte> resultList = new LinkedList<>();
            int i = 0;

            while (i < data.length) {
                int offset = (((data[i]) & 0xFF) << 8) | (data[i + 1] & 0xFF); // Savieno pirmo un otro baitu nobīdei
                int length = data[i + 2] & 0xFF; // & 0xFF nodrošina, ka baita vērtība ir pozitīva un mazāka par 255
                byte nextChar = data[i + 3];
                i += 4;

                 // Ja nav sakritības, pievienojam nākamo rakstzīmi rezultātu sarakstam
                if (offset == 0 && length == 0) {
                    resultList.add(nextChar);
                } else {
                    // Ja ir sakritība, tad kopējam datus no iepriekš saglabātiem datiem

                    int startingPointer = resultList.size() - offset;
                    int endingPointer = startingPointer + length;

                    for (int j = startingPointer; j < endingPointer; j++) {
                        if (j >= 0 && j < resultList.size()) {
                            byte character = resultList.get(j);
                            resultList.add(character);
                        }
                    }
                    resultList.add(nextChar);
                }
            }

            //Rezūltātu pierakstīšana failā
            FileOutputStream out = new FileOutputStream(resultFile);

            for (byte b : resultList) {
                out.write(b);
            }
            out.close();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}

class LZ77Token {
    int offset;
    int length;
    byte nextChar;

    public LZ77Token(int offset, int length, byte nextChar) {
        this.offset = offset;
        this.length = length;
        this.nextChar = nextChar;
    }
}
