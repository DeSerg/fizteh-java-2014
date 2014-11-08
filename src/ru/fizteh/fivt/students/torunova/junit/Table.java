package ru.fizteh.fivt.students.torunova.junit;
import ru.fizteh.fivt.students.torunova.junit.exceptions.IncorrectFileException;
import ru.fizteh.fivt.students.torunova.junit.exceptions.TableNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by nastya on 19.10.14.
 */
public class Table implements ru.fizteh.fivt.storage.strings.Table{
    public static final int NUMBER_OF_PARTITIONS = 16;
    String tableName;
    Map<File, FileMap> files = new HashMap<>();
    int numberOfEntries;

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof Table)) {
            return false;
        }
        Table table = (Table) t;
        return tableName.equals(table.tableName)
                && files.equals(table.files)
                && numberOfEntries == table.numberOfEntries;
    }

    @Override
    public int hashCode() {
        return tableName.hashCode();
    }

    public Table(String newTableName) throws TableNotCreatedException, IncorrectFileException, IOException {
        File table = new File(newTableName).getAbsoluteFile();
        if (!table.exists()) {
            if (!table.mkdirs()) {
                throw new TableNotCreatedException();
            }
        }
        tableName = table.getAbsolutePath();
        File[] tableFiles = table.listFiles();
        File[] filesOfDir;
        for(File nextDir: tableFiles) {
            if(!(Pattern.matches("[0-15].dir", nextDir.getParentFile().getName()) && nextDir.isDirectory()) ) {
                throw new TableNotCreatedException("Table " + getName() + " contains illegal files.");
            }
            filesOfDir = nextDir.listFiles();
            for(File nextFile:filesOfDir) {
                if (!(Pattern.matches("[0-15].dat",nextFile.getName()) && nextFile.isFile() )) {
                    throw new TableNotCreatedException("Table " + getName() + " contains illegal files.");
                }
                FileMap fm = new FileMap(nextFile.getAbsolutePath());
                if (fm.isEmpty()) {
                    nextFile.delete();
                    nextDir = nextFile.getParentFile();
                    if (nextDir.listFiles().length == 0)
                        nextDir.delete();
                } else {
                    files.put(nextFile, fm);
                    numberOfEntries += fm.size();
                }
            }

        }
        /*for (int i = 0; i < 16; i++) {
            nextDir = new File(table, String.valueOf(i) + ".dir").getAbsoluteFile();
            if (nextDir.isDirectory()) {
                for (int j = 0; j < 16; j++) {
                    nextFile = new File(nextDir, String.valueOf(j) + ".dat");
                    if (nextFile.isFile()) {
                        FileMap fm = new FileMap(nextFile.getAbsolutePath());
                        if (fm.isEmpty()) {
                            nextFile.delete();
                            nextDir.delete();
                        } else {
                            files.put(nextFile, fm);
                            numberOfEntries += fm.size();
                        }
                    }
                }
            }
        }*/
    }
    @Override
    public String put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key or value is null");
        }
        String result;
        String fileName = getFileName(key);
        String dirName = getDirName(key);
        File dir  = new File(tableName, dirName).getAbsoluteFile();
        File file = new File(dir, fileName).getAbsoluteFile();
        if (files.containsKey(file)) {
            result = files.get(file).put(key, value);
        } else {
                file.getParentFile().mkdirs();
            FileMap fm = null;
            try {
                file.createNewFile();
                fm = new FileMap(file.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (IncorrectFileException e1) {
                throw new RuntimeException(e1);
            }
                result = fm.put(key, value);
                files.put(file, fm);
        }
        if (result == null) {
            numberOfEntries++;
        }
        return result;
    }

    @Override
    public String getName() {
        File ourTable = new File(tableName);
        return ourTable.getName();
    }

    @Override
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key is null");
        }
        String fileName = getFileName(key);
        String dirName = getDirName(key);
        File dir  = new File(tableName, dirName).getAbsoluteFile();
        File file = new File(dir, fileName).getAbsoluteFile();
        if ((!dir.exists()) || (!file.exists())) {
            return null;
        }
        FileMap fm;
        fm = files.get(file);
        if (fm == null) {
            return null;
        }
        return fm.get(key);
    }
    @Override
    public String remove(String key) {
        String result;
        String fileName = getFileName(key);
        String dirName = getDirName(key);
        File dir  = new File(tableName, dirName).getAbsoluteFile();
        File file = new File(dir, fileName).getAbsoluteFile();
        if (files.containsKey(file)) {
            FileMap fm = files.get(file);
            result = fm.remove(key);
            numberOfEntries--;
            return result;
        }
        return null;
    }

    @Override
    public int size() {
        return numberOfEntries;
    }

    public List<String> list() {
        List<String> listOfAllKeys = new ArrayList<>();
        for (FileMap fm : files.values()) {
            listOfAllKeys.addAll(fm.list());
        }
        return listOfAllKeys;
    }
    @Override
    public int commit()  {
        int numberOfChangedEntries = 0;
        Set<Map.Entry<File, FileMap>> entrySet = new HashSet<>(files.entrySet());
        for (Map.Entry<File, FileMap> entry : entrySet) {
            FileMap fm = entry.getValue();
            File file = entry.getKey();
            try {
                numberOfChangedEntries += fm.commit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (fm.isEmpty()) {
                File directory = file.getParentFile().getAbsoluteFile();
                file.delete();
                if (directory.listFiles().length == 0) {
                    directory.delete();
                }
                files.remove(file);
            }
        }
        return numberOfChangedEntries;
    }

    @Override
    public int rollback() {
        int numberOfRevertedChanges = 0;
        numberOfEntries = 0;
        for (FileMap fm:files.values()) {
            numberOfRevertedChanges += fm.rollback();
            numberOfEntries += fm.size();
        }
        return numberOfRevertedChanges;
    }
    public int countChangedEntries() {
        int numberOfChangedEntries = 0;
        for (FileMap fm:files.values()) {
            numberOfChangedEntries += fm.countChangedEntries();
        }
        return numberOfChangedEntries;
    }

    private String getDirName(String key) {
        int hashcode = Math.abs(key.hashCode());
        int ndirectory = hashcode % NUMBER_OF_PARTITIONS;
        StringBuilder builder = new StringBuilder();
        builder.append(ndirectory).append(".dir");
        return builder.toString();
    }

    private String getFileName(String key) {
        int hashcode = Math.abs(key.hashCode());
        int nfile = hashcode / NUMBER_OF_PARTITIONS % NUMBER_OF_PARTITIONS;
        StringBuilder builder = new StringBuilder();
        builder.append(nfile).append(".dat");
        return  builder.toString();

    }

}
