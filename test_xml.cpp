#include <iostream>
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QXmlStreamWriter"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QBuffer"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QString"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QByteArray"

int main() {
    QByteArray ba;
    QBuffer buf(&ba);
    buf.open(QIODevice::WriteOnly);
    QXmlStreamWriter w(&buf);
    w.writeStartDocument();
    w.writeStartElement("KeyFile");
    w.writeStartElement("Meta");
    w.writeTextElement("Version", "2.0");
    w.writeEndElement();
    w.writeEndDocument();
    
    std::cout << ba.data() << std::endl;
    return 0;
}
