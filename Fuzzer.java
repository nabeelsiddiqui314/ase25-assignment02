import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Random;

public class Fuzzer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Fuzzer.java \"<command_to_fuzz>\"");
            System.exit(1);
        }
        String commandToFuzz = args[0];
        String workingDirectory = "./";

        if (!Files.exists(Paths.get(workingDirectory, commandToFuzz))) {
            throw new RuntimeException("Could not find command '%s'.".formatted(commandToFuzz));
        }
	
	String seedInput = "";
	
	try {
             seedInput = Files.readString(Path.of("sample.html"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        
        

        ProcessBuilder builder = getProcessBuilderForCommand(commandToFuzz, workingDirectory);
        System.out.printf("Command: %s\n", builder.command());

        runCommand(builder, seedInput, getMutatedInputs(seedInput, MutatorFactory.getRandomMutators(1000)));
    }

    private static ProcessBuilder getProcessBuilderForCommand(String command, String workingDirectory) {
        ProcessBuilder builder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        builder.directory(new File(workingDirectory));
        builder.redirectErrorStream(true); // redirect stderr to stdout
        return builder;
    }

    private static void runCommand(ProcessBuilder builder, String seedInput, List<String> mutatedInputs) {
        Stream.concat(Stream.of(seedInput), mutatedInputs.stream()).forEach(
                input -> {
                    try {
                        Process process = builder.start();
                        OutputStream stdin = process.getOutputStream();
                    
                        stdin.write(input.getBytes());
                        stdin.flush();
                        stdin.close();
                    
                        int exitCode = process.waitFor();
                    
                        if (exitCode == 0) {
                    	    //System.out.println("Exited with code 0 for input: " + input);
                        }
                        else {
                    	    //System.out.println("Crashed for input: " + input);
                    	
                    	    InputStream stdout = process.getInputStream();
                            String output = readStreamIntoString(stdout);
                        
                            System.out.println(output);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.getMessage());	
                    }
                }
        );
    }

    private static String readStreamIntoString(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .map(line -> line + System.lineSeparator())
                .collect(Collectors.joining());
    }

    private static List<String> getMutatedInputs(String seedInput, Collection<Function<String, String>> mutators) {
        return mutators.stream()
                .map(mutator -> mutator.apply(seedInput))
                .collect(Collectors.toList());
    }
}

public final class MutatorFactory {
    static ArrayList<Function<String, String>> mutatorCatalog = new ArrayList<Function<String, String>>();
    static Random rng = new Random(System.currentTimeMillis());
    
    private MutatorFactory() {}
    	
    static public ArrayList<Function<String, String>> getRandomMutators(int count) {
    	if (mutatorCatalog.isEmpty()) {
    	    mutatorCatalog.add(MutatorFactory::addRandomTagBracket);
    	    mutatorCatalog.add(MutatorFactory::deleteRandomCharacter);
    	    mutatorCatalog.add(MutatorFactory::addRandomString);
    	}
    
        var mutators = new ArrayList<Function<String, String>>();
            
        for (int i = 0; i < count; i++) {
    	    int randomIndex = rng.nextInt(mutatorCatalog.size());
            mutators.add(mutatorCatalog.get(randomIndex));
        }
    	    
    	return mutators;
    }
    
    private static int random(int bound) {
    	if (bound == 0)
    	   return 0;
    	
    	return rng.nextInt(bound);
    } 
    	
    private static String addRandomTagBracket(String input) {
        int randomPosition = random(input.length());
        int bracketType = random(3);
        
        String bracket;
        
        switch(bracketType) {
        case 0:
           bracket = "<";
           break;
        case 1:
           bracket = ">";
           break;
        default:
           bracket = "/>";
        }
        
        var stringBuilder = new StringBuilder(input);
        stringBuilder.insert(randomPosition, bracket);
        
        return stringBuilder.toString();
    }
    
    private static String deleteRandomCharacter(String input) {
        if (input.isEmpty())
            return input;
    
    	int randomPosition = random(input.length());
    	
    	var stringBuilder = new StringBuilder(input);
    	stringBuilder.deleteCharAt(randomPosition);
    	
    	return stringBuilder.toString();
    }
    
    private static String addRandomCharacter(String input) {
    	int randomPosition = random(input.length());
    	int randomAscii = random(128);
    	String asciiString = Character.toString((char)randomAscii);
    	
    	var stringBuilder = new StringBuilder(input);
        stringBuilder.insert(randomPosition, asciiString);
        
        return stringBuilder.toString();
    }
    
    private static String addRandomString(String input) {
        int randomLength = random(20) + 1;
        int randomPosition = random(input.length());
        
        String randomString = "";
        
        for (int i = 0; i < randomLength; i++) {
            int randomAscii = random(128);
            String asciiString = Character.toString((char)randomAscii);
            
            randomString += asciiString;
        }
        
        var stringBuilder = new StringBuilder(input);
        stringBuilder.insert(randomPosition, randomString);
        
        return stringBuilder.toString();
    }
}
