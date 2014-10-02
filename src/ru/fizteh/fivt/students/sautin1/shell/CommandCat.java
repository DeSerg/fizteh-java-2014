package ru.fizteh.fivt.students.sautin1.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Created by sautin1 on 9/30/14.
 */
public class CommandCat extends Command {

    public CommandCat() {
        minArgNumber = 1;
        commandName = "cat";
    }

    @Override
    public void execute(String... args) throws RuntimeException, IOException {
        if (!enoughArguments(args)) {
            throw new IllegalArgumentException(toString() + ": missing operand");
        }

        String fileName = args[1];
        Path fileAbsolutePath = presentWorkingDirectory.resolve(fileName).normalize();

        if (Files.exists(fileAbsolutePath)) {
            if (!Files.isDirectory(fileAbsolutePath)) {
                Scanner scanner = new Scanner(Files.newInputStream(fileAbsolutePath));
                while (scanner.hasNextLine()) {
                    System.out.println(scanner.nextLine());
                }
            } else {
                throw new IllegalArgumentException(toString() + ": " + fileName + ": Is a directory");
            }
        } else {
            throw new NoSuchFileException(toString() + ": " + fileName + ": No such file or directory");
        }
    }

}
