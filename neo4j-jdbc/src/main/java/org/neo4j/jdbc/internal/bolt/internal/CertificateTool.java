/*
 * Copyright (c) 2023-2024 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.jdbc.internal.bolt.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public final class CertificateTool {

	/**
	 * Load the certificates written in X.509 format in a file to a key store.
	 * @param certFiles the certificate files
	 * @param keyStore the key store
	 */
	public static void loadX509Cert(List<File> certFiles, KeyStore keyStore)
			throws GeneralSecurityException, IOException {
		var certCount = 0; // The files might contain multiple certs
		for (var certFile : certFiles) {
			try (var inputStream = new BufferedInputStream(new FileInputStream(certFile))) {
				var certFactory = CertificateFactory.getInstance("X.509");

				while (inputStream.available() > 0) {
					try {
						var cert = certFactory.generateCertificate(inputStream);
						certCount++;
						loadX509Cert(cert, "neo4j.javadriver.trustedcert." + certCount, keyStore);
					}
					catch (CertificateException ex) {
						if (ex.getCause() != null && ex.getCause().getMessage().equals("Empty input")) {
							// This happens if there is whitespace at the end of the
							// certificate - we load one cert, and
							// then try and load a
							// second cert, at which point we fail
							return;
						}
						throw new IOException("Failed to load certificate from `" + certFile.getAbsolutePath() + "`: "
								+ certCount + " : " + ex.getMessage(), ex);
					}
				}
			}
		}
	}

	public static void loadX509Cert(X509Certificate[] certificates, KeyStore keyStore) throws GeneralSecurityException {
		for (var i = 0; i < certificates.length; i++) {
			loadX509Cert(certificates[i], "neo4j.javadriver.trustedcert." + i, keyStore);
		}
	}

	/**
	 * Load a certificate to a key store with a name.
	 * @param certAlias a name to identify different certificates
	 * @param cert the certificate
	 * @param keyStore the key store
	 */
	private static void loadX509Cert(Certificate cert, String certAlias, KeyStore keyStore) throws KeyStoreException {
		keyStore.setCertificateEntry(certAlias, cert);
	}

	private CertificateTool() {
	}

}
