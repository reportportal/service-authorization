/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth;

import com.epam.ta.reportportal.database.entity.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.util.SerializationUtils;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Authentication utils
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public final class AuthUtils {

    private AuthUtils() {
        //statics only
    }

    public static final Function<UserRole, List<GrantedAuthority>> AS_AUTHORITIES = userRole -> Collections
            .singletonList(new SimpleGrantedAuthority(userRole.getAuthority()));

    /**
     * Dirty hack to fix <a href="https://github.com/spring-projects/spring-security-oauth/issues/665">Spring Security Issue</a>
     * If there is serialUid mismatch, replaces Uuid and tries de-serialize object again
     * Introduces mismatchCallback function to handle successful recovery of Uuid mismatch
     *
     * @param data             Data to de-serialize
     * @param mismatchCallback Mismatch callback. Executed in case of successful recovery
     * @param <T>              Type of Object
     * @return De-serialized object
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeSafely(byte[] data, @Nullable Consumer<T> mismatchCallback) {
        try {
            return SerializationUtils.deserialize(data);
        } catch (IllegalArgumentException e) {
            boolean serialUidMismatch = java.io.InvalidClassException.class.equals(e.getCause().getClass());
            if (!serialUidMismatch) {
                throw e;
            }

            try {
                ObjectInputStream is = new SerialUidReplacingInputStream(new ByteArrayInputStream(data));
                T t = (T) is.readObject();
                if (null != mismatchCallback) {
                    mismatchCallback.accept(t);
                }
                return t;
            } catch (IOException | ClassNotFoundException e1) {
                throw new IllegalArgumentException("Unable to serialize object", e1);
            }
        }
    }

    public static class SerialUidReplacingInputStream extends ObjectInputStream {

        private static Logger logger = LoggerFactory.getLogger(SerialUidReplacingInputStream.class);

        public SerialUidReplacingInputStream(InputStream in) throws IOException {
            super(in);
        }

        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
            Class localClass; // the class in the local JVM that this descriptor represents.
            try {
                localClass = Class.forName(resultClassDescriptor.getName());
            } catch (ClassNotFoundException e) {
                logger.error("No local class for " + resultClassDescriptor.getName(), e);
                return resultClassDescriptor;
            }
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
            if (localClassDescriptor != null) { // only if class implements serializable
                final long localSUID = localClassDescriptor.getSerialVersionUID();
                final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                    final StringBuffer s = new StringBuffer("Overriding serialized class version mismatch: ");
                    s.append("local serialVersionUID = ").append(localSUID);
                    s.append(" stream serialVersionUID = ").append(streamSUID);
                    Exception e = new InvalidClassException(s.toString());
                    logger.error("Potentially Fatal Deserialization Operation.", e);
                    resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
                }
            }
            return resultClassDescriptor;
        }
    }
}
