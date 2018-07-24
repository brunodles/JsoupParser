package com.brunodles.jsoupparser.smallanotation.nested;

import com.brunodles.jsoupparser.Transformer;
import com.brunodles.jsoupparser.smallanotation.AnnotationInvocation;
import com.brunodles.jsoupparser.smallanotation.TransformerFor;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

@TransformerFor(Nested.class)
public class NestedTransformer<OUTPUT> implements Transformer<AnnotationInvocation<Nested, Elements>, List<OUTPUT>> {

    @Override
    public List<OUTPUT> transform(AnnotationInvocation<Nested, Elements> value) {
        ArrayList<OUTPUT> result = new ArrayList<>();
        for (Element element : value.result) {
            Object object =value.proxyHandler.jsoupParser.parseElement(element, value.getMethodRealReturnType());
            result.add((OUTPUT) object);
        }
        return result;
    }
}