/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.util.completer;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.utils.Config;

import static org.aesh.utils.Config.getLineSeparator;


/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileCompleterGenerator {

    /**
     * 1. generate function for command
     * 2. if its a group command
     * 3. write functions for all the commands
     * 4. generate main function
     *
     * @param command
     * @return
     */
    public String generateCompeterFile(CommandLineParser<Command<CommandInvocation>> command) {
         StringBuilder out = new StringBuilder();

        out.append(generateHeader(command.getProcessedCommand().name()));

        if(command.isGroupCommand())
            out.append(generateArrContains());

        out.append(generateMainCompletion(command));

        out.append(generateCommand(command)) ;
        if(command.isGroupCommand())
            for(CommandLineParser child : command.getAllChildParsers())
                out.append(generateCommand(child));

        out.append(generateFooter(command.getProcessedCommand().name()));

        return out.toString();
    }

    private String generateMainCompletion(CommandLineParser<Command<CommandInvocation>> command) {
        StringBuilder main = new StringBuilder();
        main.append("function _complete_").append(command.getProcessedCommand().name().toLowerCase()).append(" {").append(getLineSeparator());
        if(command.isGroupCommand()) {
            for (int i = 0; i < command.getAllChildParsers().size(); i++) {
                main.append("  CHILD").append(i).append("=(").append(command.getAllChildParsers().get(i).getProcessedCommand().name().toLowerCase()).append(")").append(getLineSeparator());
            }
            for (int i = 0; i < command.getAllChildParsers().size(); i++) {
                main.append("  ArrContains COMP_WORDS CHILD").append(i)
                        .append(" && { _command_").append(command.getAllChildParsers().get(i).getProcessedCommand().name().toLowerCase()).append("; return $?; }").append(getLineSeparator());
            }

            main.append(Config.getLineSeparator());
        }
        main.append("  _command_").append(command.getProcessedCommand().name().toLowerCase()).append("; return $?;").append(getLineSeparator());
        main.append("}").append(getLineSeparator()).append(getLineSeparator()).append(getLineSeparator());;

        return main.toString();
    }

    private String generateCommand(CommandLineParser<Command<CommandInvocation>> command) {
        StringBuilder builder = new StringBuilder();
        builder.append("function _command_").append(command.getProcessedCommand().name().toLowerCase()).append(" {").append(getLineSeparator());
        builder.append(generateDefaultCompletionVariables());
        //child commands
        builder.append("  CHILD_COMMANDS=\"");
        if(command.isGroupCommand()) {
            for(CommandLineParser child : command.getAllChildParsers()) {
                builder.append(child.getProcessedCommand().name().toLowerCase()).append(" ");
            }
        }
        builder.append("\";").append(getLineSeparator());

        //options
        StringBuilder noValueOptions = new StringBuilder();
        noValueOptions.append("  NO_VALUE_OPTIONS=\"");
        StringBuilder valueOptions = new StringBuilder();
        valueOptions.append("  VALUE_OPTIONS=\"");
        for(ProcessedOption option : command.getProcessedCommand().getOptions()) {
            StringBuilder b;
            if(option.hasValue())
                b = valueOptions;
            else
                b = noValueOptions;
            b.append("--").append(option.name()).append(" ");
            if(option.shortName() != null)
                b.append("-").append(option.shortName()).append(" ");
        }
        noValueOptions.append("\"").append(getLineSeparator());
        valueOptions.append("\"").append(getLineSeparator());

        builder.append(noValueOptions.toString());
        builder.append(valueOptions.toString());

        //generate completion values
        builder.append( generateCompletionValues(command));

        builder.append("  COMPREPLY=( $(compgen -W \"${NO_VALUE_OPTIONS} ${VALUE_OPTIONS} ${CHILD_COMMANDS}\" -- ${CURR_WORD}) )").append(getLineSeparator());

        builder.append("}").append(getLineSeparator()).append(getLineSeparator()).append(getLineSeparator());
        return builder.toString();
    }

    private String generateCompletionValues(CommandLineParser<Command<CommandInvocation>> command) {
        StringBuilder builder = new StringBuilder();
        for(ProcessedOption option : command.getProcessedCommand().getOptions()) {
            if(option.hasValue()) {
                builder.append("  ").append(option.name().toUpperCase()).append("_VALUES=\"");
                for(String value : option.getDefaultValues())
                    builder.append(value).append(" ");
                if(option.type().getName().equalsIgnoreCase("boolean"))
                    builder.append("true false");
                builder.append("\"").append(getLineSeparator());
            }
        }
        if(builder.length() > 0) {
            builder.append(getLineSeparator());
            builder.append("  case ${CURR_WORD} in").append(getLineSeparator());
            for(ProcessedOption option : command.getProcessedCommand().getOptions()) {
                if(option.hasValue()) {
                   builder.append("    --").append(option.name().toLowerCase());
                   if(option.shortName() != null)
                       builder.append("|-").append(option.shortName());
                    builder.append(")").append(getLineSeparator());
                   builder.append("      COMPREPLY=( $( compgen -W \"${").append(option.name().toUpperCase()).append("_VALUES}\" -- \"\" ) )").append(getLineSeparator());
                   builder.append("      return $?").append(getLineSeparator()).append("      ;;").append(getLineSeparator());
                }
            }
            builder.append("    *)").append(getLineSeparator());
            builder.append("    case ${PREV_WORD} in").append(getLineSeparator());
            for(ProcessedOption option : command.getProcessedCommand().getOptions()) {
                if(option.hasValue()) {
                   builder.append("    --").append(option.name().toLowerCase());
                   if(option.shortName() != null)
                       builder.append("|-").append(option.shortName());
                    builder.append(")").append(getLineSeparator());
                   builder.append("      COMPREPLY=( $( compgen -W \"${").append(option.name().toUpperCase()).append("_VALUES}\" -- $CURR_WORD ) )").append(getLineSeparator());
                   builder.append("      return $?").append(getLineSeparator()).append("      ;;").append(getLineSeparator());
                }
            }
            builder.append("    esac").append(getLineSeparator()).append("  esac").append(getLineSeparator()).append(getLineSeparator());

        }

        return builder.toString();
    }

    private String generateHeader(String name) {
        StringBuilder header = new StringBuilder();
        header.append("#!/usr/bin/env bash"+getLineSeparator()+getLineSeparator())
                .append("#------------------------------------------")
                .append(getLineSeparator())
                .append("# Completion support for ").append(name).append(" generated by Aesh.")
                .append(getLineSeparator())
                .append("#------------------------------------------")
                .append(getLineSeparator())
                .append(getLineSeparator())
                .append("if [ -n \"$BASH_VERSION\" ]; then").append(getLineSeparator())
                .append("  shopt -s progcomp").append(getLineSeparator())
                .append("elif [ -n \"$ZSH_VERSION\" ]; then").append(getLineSeparator())
                .append("  setopt COMPLETE_ALIASES").append(getLineSeparator())
                .append("  alias compopt=complete").append(getLineSeparator())
                .append("fi").append(getLineSeparator()).append(Config.getLineSeparator());


        return header.toString();
    }

    private String generateFooter(String name) {
        StringBuilder builder = new StringBuilder();
        builder.append("complete -F _complete_").append(name).append(" -o default ").append(name).append(" ").append(name).append(".sh");
        return builder.toString();
    }

    private String generateArrContains() {
        return "function ArrContains() {"+getLineSeparator()+
                "  local lArr1 lArr2"+getLineSeparator()+
                "  declare -A tmp"+getLineSeparator()+
                "  eval lArr1=(\"\\\"\\${$1[@]}\\\"\")"+getLineSeparator()+
                "  eval lArr2=(\"\\\"\\${$2[@]}\\\"\")"+getLineSeparator()+
                "  for i in \"${lArr1[@]}\";{ [ -n \"$i\" ] && ((++tmp[$i]));}"+getLineSeparator()+
                "  for i in \"${lArr2[@]}\";{ [ -n \"$i\" ] && [ -z \"${tmp[$i]}\" ] && return 1;}"+getLineSeparator()+
                "  return 0"+getLineSeparator()+
                "}"+getLineSeparator()+getLineSeparator();
    }

    private String generateDefaultCompletionVariables() {
        return "# Specify current and previous word" + getLineSeparator() +
                "  CURR_WORD=${COMP_WORDS[COMP_CWORD]}" + getLineSeparator() +
                "  PREV_WORD=${COMP_WORDS[COMP_CWORD-1]}" + getLineSeparator();
    }


}
