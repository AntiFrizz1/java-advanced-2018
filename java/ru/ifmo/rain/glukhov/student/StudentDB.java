package ru.ifmo.rain.glukhov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getFieldByFunction(students.stream(), Student::getFirstName).collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getFieldByFunction(students.stream(), student -> student.getFirstName() + " " + student.getLastName())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getFieldByFunction(students.stream(), Student::getGroup).collect(Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getFieldByFunction(students.stream(), Student::getLastName).collect(Collectors.toList());
    }

    private Stream<String> getFieldByFunction(Stream<Student> stream, Function<Student, String> field) {
        return stream.map(field);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortedFindStudentsByField(students.stream(), student -> student.getFirstName().equals(name))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return sortedFindStudentsByField(students.stream(), student -> student.getGroup().equals(group))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortedFindStudentsByField(students.stream(), student -> student.getLastName().equals(name))
                .collect(Collectors.toList());
    }

    private Comparator<Student> getComparator() {
        return Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName).thenComparing(Student::getId);
    }

    private Stream<Student> sortedFindStudentsByField(Stream<Student> stream, Predicate<Student> predcate) {
        return stream.filter(predcate).sorted(getComparator());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentByComparator(students.stream(), Student::compareTo).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentByComparator(students.stream(), getComparator()).collect(Collectors.toList());
    }

    private Stream<Student> sortStudentByComparator(Stream<Student> stream, Comparator<Student> comparator) {
        return stream.sorted(comparator);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream().filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFieldByFunction(students.stream(), Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

}
