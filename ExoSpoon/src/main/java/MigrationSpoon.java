import library.Cluster;
import library.Pair;
import library.Triplet;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MigrationSpoon {
    public static final String projectPath = "/Users/benjaminadolphe/Desktop/fac/M2/S1 - local/HAI913I-Evolution-et-restructuration-des-logiciels/SootTutorial";
    public static final String projectSourcePath = projectPath + "/src";

    static SpoonAPI spoon;

    static List<CtClass> allClasses = new ArrayList<>();
    static List<String> allClassesNames = new ArrayList<>();
    static List<String> grapheCouplageList = new ArrayList<>();

    static int allRelationsCounter = 0;
    static int countLevel = 0;

    static HashMap<Pair<String, String>, Float> couplageMap = new HashMap<>();


    static List<CtClass> classToCreateProxy = new ArrayList<>();


    public static void main(String[] args) {
        final File folder = new File(projectSourcePath);
        spoon = new Launcher();
        spoon.addInputResource(folder.getAbsolutePath());
        spoon.buildModel();
        getAllClass();
        countAllRelations();
        System.out.println("Nombre de relations = " + allRelationsCounter);
        createListGraphePondere();
        createGraphePondere();
        Cluster dendro = clusteringHierarchique();
        System.out.println("Cluster Hiérarchique : \n" + dendro);
        List<Cluster> clusters = selection_cluster(dendro);
        for (Cluster cluster : clusters) {
            System.out.println("Un cluster : \n" + cluster);
        }
        checkDependancies(clusters);
    }

    private static void getAllClass() {
        for (CtClass javaClass : spoon.getModel().getElements(new TypeFilter<>(CtClass.class))) {
            if (javaClass.isEnum() || javaClass.isInterface() || javaClass.isAbstract() || javaClass.isAnonymous())
                continue;
            allClasses.add(javaClass);
            allClassesNames.add(javaClass.getQualifiedName());
        }
    }

    private static void countAllRelations() {
        for (CtClass javaClass : allClasses) {
            for (Object method : javaClass.getMethods()) {
                CtMethod currentMethod = (CtMethod) method;
                for (CtInvocation methodInvocation : currentMethod.getElements(new TypeFilter<>(CtInvocation.class))) {
                    if (methodInvocation.getExecutable().getDeclaringType() == null || methodInvocation.getExecutable().getDeclaringType().getTypeDeclaration() == null)
                        continue;
                    String qualifiedName = methodInvocation.getExecutable().getDeclaringType().getTypeDeclaration().getQualifiedName();
                    if (!javaClass.getQualifiedName().equals(qualifiedName) && allClassesNames.contains(qualifiedName)) {
                        allRelationsCounter += 1;
                    }
                }
            }
        }
    }

    private static int numberOfRelationBetweenTwoClasses(CtClass A, CtClass B) {
        int numberOfRelations = 0;

        if (A.isInterface() || B.isInterface()) {
            return 0;
        }

        for (Object method : A.getMethods()) {
            CtMethod currentMethod = (CtMethod) method;
            for (CtInvocation methodInvocation : currentMethod.getElements(new TypeFilter<>(CtInvocation.class))) {
                if (methodInvocation.getExecutable().getDeclaringType() == null || methodInvocation.getExecutable().getDeclaringType().getTypeDeclaration() == null)
                    continue;
                String qualifiedName = methodInvocation.getExecutable().getDeclaringType().getTypeDeclaration().getQualifiedName();
                if (qualifiedName.equals(B.getQualifiedName())) {
                    numberOfRelations += 1;
                }
            }
        }

        for (Object method : B.getMethods()) {
            CtMethod currentMethod = (CtMethod) method;
            for (CtInvocation methodInvocation : currentMethod.getElements(new TypeFilter<>(CtInvocation.class))) {
                if (methodInvocation.getExecutable().getDeclaringType() == null || methodInvocation.getExecutable().getDeclaringType().getTypeDeclaration() == null)
                    continue;
                String qualifiedName = methodInvocation.getExecutable().getDeclaringType().getTypeDeclaration().getQualifiedName();
                if (qualifiedName.equals(A.getQualifiedName())) {
                    numberOfRelations += 1;
                }
            }
        }

        return numberOfRelations;
    }

    private static String calculCouplage(CtClass A, CtClass B) {
        if (allRelationsCounter == 0) return "0";
        System.out.println("Nombre de relation entre " + A.getQualifiedName() + " et " + B.getQualifiedName() + " est de " + numberOfRelationBetweenTwoClasses(A, B) + "/" + allRelationsCounter);
        return numberOfRelationBetweenTwoClasses(A, B) + "/" + allRelationsCounter;
    }

    private static float calculCouplageFloat(String couplage) {
        String[] elements = couplage.split("/");
        return (float) Integer.parseInt(elements[0]) / Integer.parseInt(elements[1]);
    }

    private static void createListGraphePondere() {
        for (int i = 0; i < allClasses.size(); i++) {
            for (int j = i; j < allClasses.size(); j++) {
                CtClass class1 = allClasses.get(i);
                CtClass class2 = allClasses.get(j);
                if (class2.getQualifiedName().equals(class1.getQualifiedName())) {
                    continue;
                }
                String couplageString = calculCouplage(class1, class2);
                float couplageFloat = calculCouplageFloat(couplageString);
                System.out.println("couplage btw " + class1.getQualifiedName() + " et " + class2.getQualifiedName() + " " + couplageFloat);
                DecimalFormat df = new DecimalFormat("#.####");
                couplageMap.put(new Pair<String, String>(class1.getQualifiedName(), class2.getQualifiedName()), couplageFloat);
                grapheCouplageList.add("\t" + "\"" + class1.getQualifiedName() + "\"--\"" + class2.getQualifiedName() + "\"[label=\"" + df.format(couplageFloat) + " (" + couplageString + ")" + "\"];\n");
            }
        }
    }


    private static void createGraphePondere() {
        try {
            String name = UUID.randomUUID().toString();
            FileWriter writer = new FileWriter("export/dot/graphePondere/" + name + ".dot");
            writer.write("graph \"call-graph\" {\n");
            distinct(writer, grapheCouplageList);
        } catch (IOException e) {
            System.out.println("Une erreur s'est produite.");
        }
    }

    private static void distinct(FileWriter writer, List<String> graphCouplageList) throws IOException {
        graphCouplageList.stream().distinct().collect(Collectors.toList()).forEach(couplageElement -> {
            try {
                writer.write(couplageElement);
            } catch (IOException e) {
                System.out.println("Une erreur est survenue au niveau de l'écriture des liens");
            }
        });
        writer.write("}");
        writer.close();
        System.out.println("\nun fichier a bien été créé");
    }

    private static Triplet<Cluster, Cluster, Float> clusterProche(Map<Pair<String, String>, Float> couplages, List<Cluster> clusters) {
        Pair<Cluster, Cluster> pair = new Pair<>();
        float max = 0;
        Cluster first = null;
        Cluster second = null;
        for (Cluster cluster : clusters) {
            for (Cluster cluster1 : clusters) {
                if (cluster == cluster1) continue;
                float couplage = calculCouplageBetweenClusters(couplages, cluster, cluster1);
                if (couplage >= max) {
                    max = couplage;
                    first = cluster;
                    second = cluster1;
                }
            }
        }
        pair.setKey(first);
        pair.setValue(second);
        return new Triplet<>(pair, max);
    }

    private static float calculCouplageBetweenClusters(Map<Pair<String, String>, Float> couplages, Cluster c1, Cluster c2) {
        float sum = 0;
        for (String aClass : c1.getClasses()) {
            for (String bClass : c2.getClasses()) {
                for (Map.Entry<Pair<String, String>, Float> mapEntry : couplages.entrySet()) {
                    if (mapEntry.getKey().equals(new Pair<>(aClass, bClass))) {
                        sum += mapEntry.getValue();
                    }
                }
            }
        }
        return sum;
    }

    private static ArrayList<Cluster> convertNameToClusters() {
        return new ArrayList<>(allClassesNames.stream().map(Cluster::new).toList());
    }

    private static Cluster clusteringHierarchique() {
        List<Cluster> clusters = convertNameToClusters();
        while (clusters.size() > 1) {
            Triplet<Cluster, Cluster, Float> triplet = clusterProche(couplageMap, clusters);
            Cluster c3 = new Cluster(triplet.getFirst(), triplet.getSecond(), triplet.getThird(), countLevel);
            countLevel++;
            clusters.remove(triplet.getFirst());
            clusters.remove(triplet.getSecond());
            clusters.add(c3);
        }
        return clusters.get(0);
    }

    private static List<Cluster> selection_cluster(Cluster dendro) {
        List<Cluster> R = new ArrayList<>();
        Stack<Cluster> parcoursCluster = new Stack<>();
        parcoursCluster.push(dendro);
        while (!parcoursCluster.isEmpty()) {
            Cluster pere = parcoursCluster.pop();
            Pair<Cluster, Cluster> fils = pere.getPairs();
            Cluster f1 = fils.getKey();
            Cluster f2 = fils.getValue();
            if (f1 == null || f2 == null) {
                R.add(pere);
                continue;
            }
            if (S(pere) > moyenne(S(f1, f2))) {
                R.add(pere);
            } else {
                parcoursCluster.push(f1);
                parcoursCluster.push(f2);
            }
        }
        return R;
    }

    private static float S(Cluster p) {
        float s1 = p.getValue();
        float s2 = S2(p);
        return s1 / s2;
    }

    private static float S2(Cluster p) {
        float sum = 0;
        for (String aClass : p.getClasses()) {
            for (String className : allClassesNames) {
                if (p.getClasses().contains(className)) continue;
                for (Map.Entry<Pair<String, String>, Float> entry : couplageMap.entrySet()) {
                    if (entry.getKey().equals(new Pair<>(aClass, className))) {
                        sum += entry.getValue();
                    }
                }
            }
        }
        if (sum == 0) sum = 1;
        return sum;
    }

    private static Pair<Float, Float> S(Cluster c1, Cluster c2) {
        return new Pair<>(S(c1), S(c2));
    }

    private static float moyenne(Pair<Float, Float> pair) {
        return (pair.getValue() + pair.getKey()) / 2;
    }

    private static void checkDependancies(List<Cluster> clusters){
        for (Cluster cluster : clusters) {
            
        }
    }


}
