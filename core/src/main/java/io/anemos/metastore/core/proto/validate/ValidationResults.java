package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.v1alpha1.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResults {
    Map<String, FileResultContainer> fileMap = new HashMap<>();

    Map<String, MessageResultContainer> messageMap = new HashMap<>();

    public List<RuleInfo> getInfo(String messageName, String fieldName) {
        List<RuleInfo> rules = new ArrayList<>();
        MessageResultContainer messageResult = messageMap.get(messageName);
        if (messageResult != null) {
            FieldResultContainer fieldResultContainer = messageResult.fieldMap.get(fieldName);
            if (fieldResultContainer != null) {
                rules.addAll(fieldResultContainer.info);
            }
        }
        return rules;
    }

    private MessageResultContainer getOrCreateMessage(String messageName) {
        MessageResultContainer messageResult = messageMap.get(messageName);
        if (messageResult == null) {
            messageResult = new MessageResultContainer();
            messageResult.fullName = messageName;
            messageMap.put(messageName, messageResult);
        }
        return messageResult;
    }

    private FileResultContainer getOrCreateFile(String fileName) {
        FileResultContainer fileResult = fileMap.get(fileName);
        if (fileResult == null) {
            fileResult = new FileResultContainer();
            fileResult.fullName = fileName;
            fileMap.put(fileName, fileResult);
        }
        return fileResult;
    }

    void addResult(Descriptors.FieldDescriptor fd, RuleInfo ruleInfo) {
        MessageResultContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
        messageResult.add(fd, ruleInfo);
    }

    void addResult(Descriptors.Descriptor descriptor, RuleInfo ruleInfo) {
        MessageResultContainer messageResult = getOrCreateMessage(descriptor.getFullName());
        messageResult.addResult(ruleInfo);
    }

    void setPatch(Descriptors.FieldDescriptor fd, ChangeInfo patch) {
        MessageResultContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
        messageResult.addPatch(fd, patch);
    }

    void setPatch(Descriptors.Descriptor fd, ChangeInfo patch) {
        MessageResultContainer messageResult = getOrCreateMessage(fd.getFullName());
        messageResult.setPatch(patch);
    }

    void setPatch(Descriptors.FileDescriptor fd, ChangeInfo patch) {
        FileResultContainer fileResult = getOrCreateFile(fd.getFullName());
        fileResult.setPatch(patch);
    }

    public Report getReport() {
        Report.Builder builder = Report.newBuilder();
        messageMap.values().forEach(message -> {
            builder.putMessageResults(message.fullName, message.getResult());
        });

        return builder.build();
    }

    class FieldResultContainer {
        List<RuleInfo> info = new ArrayList();
        ChangeInfo patch;
        String name;
        int number;

        public void add(RuleInfo ruleInfo) {
            info.add(ruleInfo);
        }

        public FieldResult getResult() {
            FieldResult.Builder builder = FieldResult.newBuilder()
                    .setName(name)
                    .addAllInfo(info);
            if (patch != null) {
                builder.setChange(patch);
            }
            return builder.build();
        }

        public void addPatch(ChangeInfo patch) {
            this.patch = patch;
        }
    }

    class MessageResultContainer {
        String fullName;

        List<RuleInfo> info = new ArrayList<>();
        Map<String, FieldResultContainer> fieldMap = new HashMap<>();
        ChangeInfo patch;

        public void add(Descriptors.FieldDescriptor field, RuleInfo ruleInfo) {
            FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
            fieldResultContainer.add(ruleInfo);
        }

        public void addPatch(Descriptors.FieldDescriptor field, ChangeInfo patch) {
            FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
            fieldResultContainer.addPatch(patch);
        }

        private FieldResultContainer getOrCreateFieldContainer(Descriptors.FieldDescriptor field) {
            FieldResultContainer fieldResultContainer = fieldMap.get(field.getName());
            if (fieldResultContainer == null) {
                fieldResultContainer = new FieldResultContainer();
                fieldResultContainer.name = field.getName();
                fieldResultContainer.number = field.getNumber();
                fieldMap.put(field.getName(), fieldResultContainer);
            }
            return fieldResultContainer;
        }

        MessageResult getResult() {
            MessageResult.Builder messageInfo = MessageResult.newBuilder();
            messageInfo.setName(fullName);
            if(patch != null) {
                messageInfo.setChange(patch);
            }
            fieldMap.values().forEach(field -> messageInfo.addFieldResults(field.getResult()));
            messageInfo.addAllInfo(info);
            return messageInfo.build();
        }

        public void addResult(RuleInfo ruleInfo) {
            info.add(ruleInfo);
        }

        public void setPatch(ChangeInfo patch) {
            this.patch = patch;
        }
    }

    class FileResultContainer {
        String fullName;

        List<RuleInfo> info = new ArrayList<>();
        //Map<String, FieldResultContainer> fieldMap = new HashMap<>();
        ChangeInfo patch;

        public void setPatch(ChangeInfo patch) {
            this.patch = patch;
        }


    }
}