package org.bot.script.scriptdata;

/**
 * Created by Ethan on 7/14/2017.
 */
public class ScriptData {

    public String clazz;
    public String name;
    public String server;
    public String desc;
    public double version;
    public String author;
    public SkillCategory skillCategory;
    public int scriptId;

    public ScriptData(String clazz, String name, String server, String desc, double version, String author, SkillCategory category) {
        this.clazz = clazz;
        this.name = name;
        this.server = server;
        this.desc = desc;
        this.version = version;
        this.author = author;
        this.skillCategory = category;
        this.scriptId = -1;
    }

    public String getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public String getDesc() {
        return desc;
    }

    public double getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public SkillCategory getSkillCategory() {
        return skillCategory;
    }

    public int getScriptId() {
        return scriptId;
    }
}