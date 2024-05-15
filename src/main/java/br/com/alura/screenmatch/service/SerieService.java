package br.com.alura.screenmatch.service;

import br.com.alura.screenmatch.dto.EpisodioDTO;
import br.com.alura.screenmatch.dto.SerieDTO;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SerieService {

    @Autowired
    private SerieRepository repository;

    public List<SerieDTO> obterTodasSeries(){
        return converteDados(repository.findAll());
    }

    public List<SerieDTO> obterTop5Series() {

        return  converteDados(repository.findTop5ByOrderByAvaliacaoDesc());
    }


    public List<SerieDTO> obterLancamentos() {
        return converteDados(repository.lancamentosMaisRecentes());
    }

    public SerieDTO obterPorId(Long id) {
        Optional<Serie> serie = repository.findById(id);
        if (serie.isPresent())
            return new SerieDTO(serie.get());
        return null;
    }

    public List<EpisodioDTO> obterTodasTemporadas(long id) {
        Optional<Serie> serie = repository.findById(id);
        if (serie.isPresent())
            return serie.get().getEpisodios()
                    .stream()
                    .map(s -> new EpisodioDTO(s.getTemporada(), s.getNumero(), s.getTitulo()))
                    .toList();
        return null;

    }

    private List<SerieDTO> converteDados(List<Serie> series){
        return series.stream()
                .map(SerieDTO::new)
                .toList();
    }
}
