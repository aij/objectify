package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Protobuf;
import com.googlecode.objectify.impl.Path;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;


/**
 * <p>Translator between Google Protocol Buffers and GAE datastore Blobs.</p>
 *
 * @author Ivan Jager <aij+@mrph.org>
 */
public class ProtobufTranslatorFactory implements TranslatorFactory<GeneratedMessage, Blob>
{

	@Override
	public Translator<GeneratedMessage, Blob> create(TypeKey<GeneratedMessage> tk, CreateContext ctx, Path path) {
		final Protobuf protoAnno = tk.getAnnotation(Protobuf.class);

		// We only work with @Protobuf classes
		if (protoAnno == null)
			return null;

		final Class type = (Class)tk.getType();
		final Parser<? extends GeneratedMessage> parser;
		try {
			parser = (Parser<? extends GeneratedMessage>)type.getField("PARSER").get(null);
		} catch (java.lang.NoSuchFieldException e) {
			// If there is no parser this must not be a proto. Fall back to other translators.
			return null;
		} catch (java.lang.IllegalAccessException e) {
			path.throwIllegalState("IllegalAccessException while trying to get parser for" + type.getName(), e);
			return null;
		}

		return new ValueTranslator<GeneratedMessage, Blob>(Blob.class) {
			@Override
			protected GeneratedMessage loadValue(Blob value, LoadContext ctx, Path path) throws SkipException {
				try {
					return parser.parseFrom(value.getBytes());
				} catch (com.google.protobuf.InvalidProtocolBufferException e) {
					path.throwIllegalState("Error parsing protobuf.", e);
					return null;
				}
			}

			@Override
			protected Blob saveValue(GeneratedMessage value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return new Blob(value.toByteArray());
			}
		};
	}
}
