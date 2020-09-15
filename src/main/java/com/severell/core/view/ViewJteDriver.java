package com.severell.core.view;

import com.severell.core.config.Config;
import com.severell.core.exceptions.ViewException;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.PrintWriterOutput;
import gg.jte.resolve.DirectoryCodeResolver;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;

public class ViewJteDriver extends BaseView{

    private TemplateEngine templateEngine;

    public ViewJteDriver() {
        if (Config.isLocal()) {
            DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(Path.of("src", "main", "resources", "templates"));
            templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        } else {
            templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
        }
    }

    @Override
    public void render(String template, Object object, Writer writer) throws ViewException {
        render(template, object, "templates/", writer);
    }

    @Override
    public void render(String template, Object object, String baseDir, Writer writer) throws ViewException {
        if(writer instanceof PrintWriter) {
            TemplateOutput output = new PrintWriterOutput((PrintWriter) writer);
            templateEngine.render(template, object, output);
        } else {
            throw new ViewException("Invalid writer. Needs to be instance of PrintWriter");
        }
    }
}